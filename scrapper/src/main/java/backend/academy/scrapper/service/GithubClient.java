package backend.academy.scrapper.service;

import backend.academy.scrapper.ScrapperConfig;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import backend.academy.scrapper.Storage;
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
    public LocalDateTime getUpdatedAt(String link) {
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
        return LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
    }

    @Override
    public UpdateInfo checkForUpdates(String link) {
        // Проверяем все три сущности
        LocalDateTime repoUpdated = getUpdatedAt(link);
        PRAnswer prAnswer = PRUpdatedAt(link);
        IssueAnswer issueAnswer = IssuesUpdatedAt(link);


        // Приоритет: PR -> Issue -> Repo
        if (prAnswer != null) {
            if (storage.setPR(link, prAnswer)){
                return new UpdateInfo(true, "PR", prAnswer, prAnswer.getUpdatedAt());
            }

        }
        else if (issueAnswer != null) {
            if (storage.setIssue(link, issueAnswer)){
                return new UpdateInfo(true, "ISSUE", issueAnswer, issueAnswer.getUpdatedAt());
            }

        }
        else if (repoUpdated != null) {
            if (storage.isUpdated(link, repoUpdated)){
                return new UpdateInfo(true, "REPO", null, repoUpdated);
            }

        }

        return new UpdateInfo(false, null, null, null);
    }

    public PRAnswer PRUpdatedAt(String link){
        try{
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "token " + githubToken);
            String userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:140.0) Gecko/20100101 Firefox/140.0";
            headers.set("User-Agent", userAgent);
            HttpEntity<Map> request = new HttpEntity<>(headers);
            ResponseEntity<List> response = restTemplate.exchange(make_pr_url(link), HttpMethod.GET, request, List.class);

            List<Map<String, Object>> pulls = response.getBody();
            Object updatedAt = null;
            String login = null;
            String description = null;

            if (pulls != null && !pulls.isEmpty()) {
                Map<String, Object> firstPr = pulls.getFirst();

                updatedAt = firstPr.get("updated_at");

                // Получаем login с проверкой типа
                Object userObject = firstPr.get("user");
                if (userObject instanceof Map) {
                    Map<String, Object> userMap = (Map<String, Object>) userObject;
                    login = (String) userMap.get("login");
                }
                description = (String) firstPr.get("body");
            }
            if (updatedAt == null) {
                throw new IllegalArgumentException("Missing 'updated_at' field in GitHub PR response");
            }

            logger.info("Последнее обновление в Pull Request {}, {}", link, updatedAt);

            Instant instant = Instant.parse(updatedAt.toString());
            return new PRAnswer(LocalDateTime.ofInstant(instant, ZoneOffset.UTC), login, link, description);
        }
        catch (Exception e) {
            logger.error("Ошибка при получении PR для {}: {}", link, e.getMessage());
            return null; // Возвращаем null при ошибках
        }

    }

    public IssueAnswer IssuesUpdatedAt(String link){
        try{
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "token " + githubToken);
            String userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:140.0) Gecko/20100101 Firefox/140.0";
            headers.set("User-Agent", userAgent);
            HttpEntity<Map> request = new HttpEntity<>(headers);
            ResponseEntity<List> response = restTemplate.exchange(make_issue_url(link), HttpMethod.GET, request, List.class);

            List<Map<String, Object>> issues = response.getBody();
            Object updatedAt = null;
            String login = null;
            String description = null;

            if (issues != null && !issues.isEmpty()) {
                Map<String, Object> firstPr = issues.getFirst();

                updatedAt = firstPr.get("updated_at");
                Object userObject = firstPr.get("user");
                if (userObject instanceof Map) {
                    Map<String, Object> userMap = (Map<String, Object>) userObject;
                    login = (String) userMap.get("login");
                }
                description = (String) firstPr.get("body");
            }

            if (updatedAt == null) {
                throw new IllegalArgumentException("Missing 'updated_at' field in GitHub Issue response");
            }

            logger.info("Последнее обновление в Issue {}, {}", link, updatedAt);

            Instant instant = Instant.parse(updatedAt.toString());
            return new IssueAnswer(LocalDateTime.ofInstant(instant, ZoneOffset.UTC), login, link, description);
        }  catch (Exception e) {
            logger.error("Ошибка при получении PR для {}: {}", link, e.getMessage());
            return null; // Возвращаем null при ошибках
        }

    }
}
