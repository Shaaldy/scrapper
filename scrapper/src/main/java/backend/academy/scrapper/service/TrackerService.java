package backend.academy.scrapper.service;


import backend.academy.scrapper.Storage;
import backend.academy.scrapper.api.AddLinkRequest;
import backend.academy.scrapper.api.ListLinksResponse;
import backend.academy.scrapper.api.RemoveLinkRequest;
import java.time.LocalDateTime;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
@EnableScheduling
public class TrackerService {

    protected final Logger logger = LoggerFactory.getLogger(TrackerService.class);
    protected final GithubClient githubClient;
    protected final SOClient soClient;
    protected final RestTemplate restTemplate;
    protected final Storage storage;

    @Value("http://localhost:8080/api/bot")
    private String botAPI;

    @Autowired
    public TrackerService(GithubClient githubClient, SOClient soClient, RestTemplate restTemplate, Storage storage) {
        this.githubClient = githubClient;
        this.soClient = soClient;
        this.restTemplate = restTemplate;
        this.storage = storage;
    }

    public boolean addChatId(Long chatId) {
        return storage.addUser(chatId);
    }


    public boolean removeChatId(Long chatId) {
        return storage.removeUser(chatId);
    }

    public ListLinksResponse getLinks(Long chatId) {
        return storage.getLinks(chatId);
    }

    public void addLink(Long chatId, AddLinkRequest req) {
        storage.addLink(chatId, req.getLink());
    }

    public boolean removeLink(Long chatId, RemoveLinkRequest req) {
        return storage.removeLink(chatId, req.getLink());
    }

    private boolean isUpdated(IClient client, String link) {
        LocalDateTime now = client.getUpdatedAt(link);
        return storage.isUpdated(link, now);
    }

    @Scheduled(fixedRate = 5000)
    protected void sendHTTPResponse() {
        try {
            for (String link : storage.getAllTrackedLinks()) {
                IClient client = getClientForLink(link);
                if (isUpdated(client, link)) {
                    HttpHeaders headers = new HttpHeaders();
                    headers.set("Url", link);
                    headers.set("Description", "нет описания, временная затычка");

                    HttpEntity<Set<Long>> httpEntity = new HttpEntity<>(storage.getSubscribers(link), headers);
                    restTemplate.exchange(botAPI + "/updates", HttpMethod.POST, httpEntity, Void.class);
                }
            }
        } catch (Exception e) {
            logger.error("Ошибка при отправке обновлений: {}", e.getMessage(), e);
        }
    }


    private boolean isGithubLink(String link) {
        return link.startsWith("https://github.com/");
    }

    private boolean isStackoverflow(String link) {
        return link.startsWith("https://ru.stackoverflow.com");
    }

    private IClient getClientForLink(String link) {
        if (isGithubLink(link)) {
            return githubClient;
        } else if (isStackoverflow(link)) {
            return soClient;
        } else {
            throw new UnsupportedOperationException("Unsupported link type: " + link);
        }
    }


}
