package backend.academy.scrapper.service;

import backend.academy.scrapper.ScrapperConfig;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Map;
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


    @Autowired
    public GithubClient(ScrapperConfig scrapperConfig) {
        this.restTemplate = scrapperConfig.restTemplate();
        this.githubToken = scrapperConfig.githubToken();
    }

    private String make_url(String link) {
        String info = link.substring(19);
        return "https://api.github.com/repos/" + info;
    }

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
            throw new IllegalArgumentException("Missing 'updated_at' field in GitHub API response");
        }

        logger.info("Последнее обновление {}, {}", link, updatedAt);

        Instant instant = Instant.parse(updatedAt.toString());
        return LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
    }


}
