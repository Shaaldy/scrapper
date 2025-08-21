package backend.academy.scrapper.service;


import backend.academy.scrapper.Storage;
import backend.academy.scrapper.api.AddLinkRequest;
import backend.academy.scrapper.api.ListLinksResponse;
import backend.academy.scrapper.api.RemoveLinkRequest;
import java.util.Set;
import backend.academy.scrapper.service.clients.ClientAnswer;
import backend.academy.scrapper.service.clients.github.GithubClient;
import backend.academy.scrapper.service.clients.stackoverflow.SOClient;
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


    @Scheduled(fixedRate = 5000)
    protected void sendHTTPResponse() {
        try {
            for (String link : storage.getAllTrackedLinks()) {
                IClient client = getClientForLink(link);
                ClientAnswer clientAnswer = client.checkForUpdates(link);

                if (clientAnswer.hasUpdates()) {
                    logger.info("Отправляется ответ клиенту");
                    HttpHeaders headers = createHeaders(clientAnswer, link);
                    HttpEntity<Set<Long>> httpEntity = new HttpEntity<>(
                        storage.getSubscribers(link),
                        headers
                    );
                    restTemplate.exchange(botAPI + "/updates", HttpMethod.POST, httpEntity, Void.class);

                }
            }
        } catch (Exception e) {
            logger.error("Ошибка при отправке обновлений: {}", e.getMessage(), e);
        }
    }

    private HttpHeaders createHeaders(ClientAnswer clientAnswer, String link) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("url", link);  // ← lowercase
        headers.set("update_type", clientAnswer.getAnswer().toString());  // ← подчеркивание
        headers.set("updated_at", clientAnswer.getUpdatedAt().toString());  // ← lowercase

        if (clientAnswer.getAnswer() != Answer.SO || clientAnswer.getAnswer() != Answer.PR) {
            populateGitHubHeaders(headers, clientAnswer);
        }

        return headers;
    }

    private void populateGitHubHeaders(HttpHeaders headers, ClientAnswer answer) {
        headers.set("description", answer.getDescription());
        headers.set("author", answer.getAuthor());
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
