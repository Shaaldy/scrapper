package backend.academy.scrapper.service.stackoverflow;

import backend.academy.scrapper.ScrapperConfig;
import backend.academy.scrapper.ScrapperConfig.StackOverflowCredentials;
import backend.academy.scrapper.bd.StorageProvider;
import backend.academy.scrapper.service.Answer;
import backend.academy.scrapper.service.IClient;
import backend.academy.scrapper.service.github.GithubClient;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.InvalidUrlException;


@Component
public class SOClient implements IClient {
    private static final Pattern QUESTION_ID_PATTERN = Pattern.compile("questions/([0-9]+)/");
    private final String userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:140.0) Gecko/20100101 Firefox/140.0";
    private final Logger logger = LoggerFactory.getLogger(GithubClient.class);
    private final RestTemplate restTemplate;
    private final StackOverflowCredentials stackOverflowCredentials;
    private final StorageProvider storage;

    @Autowired
    public SOClient(ScrapperConfig scrapperConfig, StorageProvider storage) {
        this.restTemplate = scrapperConfig.restTemplate();
        this.stackOverflowCredentials = scrapperConfig.stackOverflow();
        this.storage = storage;
    }

    private String extractQuestionId(String url) {
        Matcher matcher = QUESTION_ID_PATTERN.matcher(url);
        if (matcher.find()) {
            return matcher.group(1);
        } else {
            logger.error("Invalid URL: {}", url);
            throw new InvalidUrlException("Invalid URL");
        }
    }

    private String makeUrl(String url, String type) {
        String id = extractQuestionId(url);
        return switch (type) {
            case "answers" ->
                "https://api.stackexchange.com/2.3/questions/" + id + "/answers?site=ru.stackoverflow&filter=withbody";
            case "comments" ->
                "https://api.stackexchange.com/2.3/questions/" + id + "/comments?site=ru.stackoverflow&filter=withbody";
            case "title" ->
                "https://api.stackexchange.com/2.3/questions/" + id + "/?site=ru.stackoverflow&filter=withbody";
            default -> throw new IllegalArgumentException("Unknown request type: " + type);
        };
    }

    private String makeUrlRequest(String url) {
        return makeUrl(url, "answers");
    }

    private String makeUrlComments(String url) {
        return makeUrl(url, "comments");
    }

    private String makeUrlTitle(String url) {
        return makeUrl(url, "title");
    }


    @SuppressWarnings("unchecked")
    private Optional<Map<String, Object>> fetchFirstItem(String url) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("User-Agent", userAgent);
            headers.set("Authorization", stackOverflowCredentials.accessToken());
            HttpEntity<Map> entity = new HttpEntity<>(headers);

            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);
            Map<String, Object> body = response.getBody();

            if (body == null || !body.containsKey("items")) {
                return Optional.empty();
            }

            List<Map> items = (List<Map>) body.get("items");
            if (items == null || items.isEmpty()) {
                return Optional.empty();
            }

            return Optional.of(items.getFirst());
        } catch (Exception e) {
            logger.error("Error fetching data from {}", url, e);
            throw new RuntimeException("Error fetching data from " + url, e);
        }
    }


    private String getTitle(String url) {
        Optional<Map<String, Object>> owner = fetchFirstItem(makeUrlTitle(url));
        return (String) owner.get().get("title");
    }


    public SOAnswer getSOR(String link, String title) {
        Optional<Map<String, Object>> optionalOwner = fetchFirstItem(makeUrlRequest(link));
        if (optionalOwner.isEmpty()) {
            logger.info("Нет ответов по ссылке {}", link);
            return new SOAnswer(false);
        }

        Map<String, Object> owner = optionalOwner.get();
        Integer lastActivity = (Integer) owner.get("last_activity_date");
        if (lastActivity == null) {
            return new SOAnswer(false);
        }

        String text = cleanText((String) owner.get("body"));
        Map user = (Map) owner.get("owner");
        String author = user != null ? (String) user.get("display_name") : null;

        Instant instant = Instant.ofEpochSecond(lastActivity);
        logger.info("Последняя активность ссылки {} - получено {}", link, lastActivity);

        return new SORequestAnswer(LocalDateTime.ofInstant(instant, ZoneOffset.UTC), author, link, title, text, Answer.SO_REQUEST);
    }


    public SOCommentAnswer getSOC(String link, String title) {
        Optional<Map<String, Object>> optionalOwner = fetchFirstItem(makeUrlComments(link));
        if (optionalOwner.isEmpty()) {
            logger.info("Нет комментариев по ссылке {}", link);
            return new SOCommentAnswer(null, null, link, title, null, Answer.SO_COMMENT);
        }

        Map<String, Object> owner = optionalOwner.get();
        Integer creationDate = (Integer) owner.get("creation_date");
        if (creationDate == null) {
            return new SOCommentAnswer(null, null, link, title, null, Answer.SO_COMMENT);
        }

        String text = cleanText((String) owner.get("body"));
        Map user = (Map) owner.get("owner");
        String author = user != null ? (String) user.get("display_name") : null;

        Instant instant = Instant.ofEpochSecond(creationDate);
        logger.info("Последняя активность ссылки {} - получено {}", link, creationDate);

        return new SOCommentAnswer(LocalDateTime.ofInstant(instant, ZoneOffset.UTC), author, link, title, text, Answer.SO_COMMENT);
    }


    private String cleanText(String text) {
        if (text == null) return "";
        text = text.replaceAll("<[^>]*>", "");
        if (text.length() > 200) {
            text = text.substring(0, 200) + "...";
        }
        return text;
    }

    @Override
    public SOAnswer checkForUpdates(String link) {
        String title = getTitle(link);
        Object sor = getSOR(link, title);
        if (sor instanceof SORequestAnswer) {
            if (storage.updateTime(link, ((SORequestAnswer) sor).getUpdatedAt())) {
                ((SORequestAnswer) sor).setHasUpdate(true);
                return (SORequestAnswer) sor;
            }
        }

        SOCommentAnswer soc = getSOC(link, title);
        if (soc != null) {
            if (storage.updateTime(link, soc.getUpdatedAt())) {
                soc.setHasUpdate(true);
                return soc;
            }
        }
        return new SOAnswer(false);
    }

}
