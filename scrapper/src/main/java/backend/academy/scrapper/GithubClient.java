package backend.academy.scrapper;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.net.http.HttpHeaders;

@Service
class GithubClient {

    private RestTemplate restTemplate;
    private String githubToken;


    @Autowired
    public GithubClient(ScrapperConfig scrapperConfig) {
        this.restTemplate = restTemplate;
        this.githubToken = scrapperConfig.githubToken();
    }

    private String make_url(String link) {
        String info = link.substring(19);
        return "https://api.github.com/repos/" + info;
    }

}
