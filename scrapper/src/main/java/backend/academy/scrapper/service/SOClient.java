package backend.academy.scrapper.service;

import backend.academy.scrapper.ScrapperConfig;
import backend.academy.scrapper.ScrapperConfig.StackOverflowCredentials;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;


@Component
public class SOClient implements IClient {
    private final String userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:140.0) Gecko/20100101 Firefox/140.0";
    private final Logger logger = LoggerFactory.getLogger(GithubClient.class);
    private final RestTemplate restTemplate;
    private final StackOverflowCredentials stackOverflowCredentials;

    @Autowired
    public SOClient(ScrapperConfig scrapperConfig) {
        this.restTemplate = scrapperConfig.restTemplate();
        this.stackOverflowCredentials = scrapperConfig.stackOverflow();
    }

    private String makeUrl(String url) {
        Pattern pattern = Pattern.compile("questions/([0-9]+)/");
        Matcher matcher = pattern.matcher(url);
        if (matcher.find()) {
            String id = matcher.group(1);
            return "https://api.stackexchange.com/2.3/questions/" + id + "/answers?site=ru.stackoverflow&sort=activity";
        } else {
            logger.error("Invalid URL: " + url);
            throw new RuntimeException("Invalid URL");
        }
    }

    public LocalDateTime getUpdatedAt(String link) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("User-Agent", userAgent);
            headers.set("Authorization", stackOverflowCredentials.accessToken());
            HttpEntity<Map> entity = new HttpEntity<>(headers);
            entity = restTemplate.exchange(makeUrl(link), HttpMethod.GET, entity, Map.class);
            Map<String, Object> body = entity.getBody();
            List<Map> owners = (List<Map>) body.get("items");
            Integer lastActivity = (Integer) owners.getFirst().get("last_activity_date");
            logger.info("Последняя активность ссылки {} - получено {}", link, lastActivity);
            Instant instant = Instant.ofEpochSecond(lastActivity);
            return LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
        } catch (Exception e) {
            logger.error("Error getting last activity on link {}", link, e);
            throw new RuntimeException("Error getting last activity on link " + link, e);
        }
    }

}
