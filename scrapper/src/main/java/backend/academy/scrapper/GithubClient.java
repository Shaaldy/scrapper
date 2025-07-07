package backend.academy.scrapper;

import java.time.LocalDateTime;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import static backend.academy.scrapper.TrackerService.ISO_INSTANT;


@Service
class GithubClient {
    private final String userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:140.0) Gecko/20100101 Firefox/140.0";
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

    public LocalDateTime updateTime(String link) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "token " + githubToken);
        headers.set("User-Agent", userAgent);
        HttpEntity<Map> request = new HttpEntity<>(headers);
        request = restTemplate.exchange(make_url(link), HttpMethod.GET, request, Map.class);
        logger.info(request.getBody().toString());
        Map<String, Object> body = request.getBody();
        logger.info(body.get("updated_at").toString());
        return LocalDateTime.parse((String) body.get("updated_at"), ISO_INSTANT);
    }

}
