package backend.academy.scrapper.service.clients.github;

import backend.academy.scrapper.ScrapperConfig;
import backend.academy.scrapper.Storage;
import backend.academy.scrapper.service.Answer;
import backend.academy.scrapper.service.IClient;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;


@Component
public class GithubClient implements IClient {
    private final Logger logger = LoggerFactory.getLogger(GithubClient.class);
    private final RestTemplate restTemplate;
    private final String githubToken;
    private final Storage storage;

    @Autowired
    public GithubClient(ScrapperConfig scrapperConfig, Storage storage) {
        this.restTemplate = scrapperConfig.restTemplate();
        this.githubToken = scrapperConfig.githubToken();
        this.storage = storage;
    }

    public String make_url(String link) {
        String info = link.substring(19);
        return "https://api.github.com/repos/" + info;
    }

    public String make_pr_url(String link) {
        String info = link.substring(19);
        return "https://api.github.com/repos/" + info + "/pulls";
    }

    public String make_issue_url(String link) {
        String info = link.substring(19);
        return "https://api.github.com/repos/" + info + "/issues";
    }




    @Override
    public GithubAnswer checkForUpdates(String link) {


        PRAnswer prAnswer = PRUpdatedAt(link);
        if (prAnswer != null) {
            if (storage.setPR(link, prAnswer)) {
                return prAnswer;
            }

        }
        IssueAnswer issueAnswer = IssuesUpdatedAt(link);

        if (issueAnswer != null) {
            if (storage.setIssue(link, issueAnswer)) {
                return issueAnswer;
            }

        }

        RepoAnswer repoAnswer = repoUpdatedAt(link);
        if (repoAnswer != null) {
            if (storage.setRepo(link, repoAnswer)) {
                return repoAnswer;
            }

        }

        return new GithubAnswer(false);
    }

    public PRAnswer PRUpdatedAt(String link) {
        return fetchUpdatedAt(
            link,
            data -> new PRAnswer(
                (LocalDateTime) data.get("updatedAt"),
                (String) data.get("login"),
                (String) data.get("link"),
                (String) data.get("description"),
                Answer.PR,
                true
            ),
            this::make_pr_url,
            "Pull Request"
        );
    }

    public IssueAnswer IssuesUpdatedAt(String link) {
        return fetchUpdatedAt(
            link,
            data -> new IssueAnswer(
                (LocalDateTime) data.get("updatedAt"),
                (String) data.get("login"),
                (String) data.get("link"),
                (String) data.get("description"),
                Answer.ISSUE,
                true
            ),
            this::make_issue_url,
            "Issue"
        );
    }

    public RepoAnswer repoUpdatedAt(String link) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "token " + githubToken);
        String userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:140.0) Gecko/20100101 Firefox/140.0";
        headers.set("User-Agent", userAgent);
        HttpEntity<Map> request = new HttpEntity<>(headers);
        ResponseEntity<Map> response = restTemplate.exchange(make_url(link), HttpMethod.GET, request, Map.class);

        Map<String, Object> body = response.getBody();
        Object updatedAt = body != null ? body.get("updated_at") : null;

        if (updatedAt == null) {
            throw new IllegalArgumentException("Missing 'updated_at' field in GitHub API REPO response");
        }

        logger.info("Последнее обновление в REPO {}, {}", link, updatedAt);

        Instant instant = Instant.parse(updatedAt.toString());
        return new RepoAnswer(LocalDateTime.ofInstant(instant, ZoneOffset.UTC), Answer.REPO, true);
    }

    private <T> T fetchUpdatedAt(
        String link,
        Function<Map<String, Object>, T> mapper,
        Function<String, String> urlMaker,
        String entityName
    ) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "token " + githubToken);
            headers.set("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:140.0) Gecko/20100101 Firefox/140.0");

            HttpEntity<Map> request = new HttpEntity<>(headers);
            ResponseEntity<List> response = restTemplate.exchange(
                urlMaker.apply(link), HttpMethod.GET, request, List.class);

            List<Map<String, Object>> entities = response.getBody();
            Object updatedAt = null;
            String login = null;
            String description = null;

            if (entities != null && !entities.isEmpty()) {
                Map<String, Object> firstEntity = entities.getFirst();
                updatedAt = firstEntity.get("updated_at");

                Object userObject = firstEntity.get("user");
                if (userObject instanceof Map<?, ?> userMap) {
                    login = (String) userMap.get("login");
                }
                description = (String) firstEntity.get("body");
            }

            if (updatedAt == null) {
                throw new IllegalArgumentException("Missing 'updated_at' field in GitHub " + entityName + " response");
            }

            logger.info("Последнее обновление в {} {}, {}", entityName, link, updatedAt);

            Instant instant = Instant.parse(updatedAt.toString());
            return mapper.apply(Map.of(
                "updatedAt", LocalDateTime.ofInstant(instant, ZoneOffset.UTC),
                "login", login,
                "link", link,
                "description", description
            ));
        } catch (Exception e) {
            logger.error("Ошибка при получении {} для {}: {}", entityName, link, e.getMessage());
            return null;
        }
    }

}
