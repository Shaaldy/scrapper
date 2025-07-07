package backend.academy.scrapper;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class TrackerService {
    public static DateTimeFormatter ISO_INSTANT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");
    Logger logger = Logger.getLogger(TrackerService.class.getName());
    GithubClient githubClient;
    Map<Long, ListLinksResponse> trackedLinks = new HashMap<>();
    Set<Long> chatIds = new HashSet<>();
    Map<String, LocalDateTime> lastUpdated = new HashMap<>();

    @Autowired
    public TrackerService(GithubClient githubClient) {
        this.githubClient = githubClient;
    }

    public boolean addChatId(Long chatId) {
        chatIds.add(chatId);
        return true;
    }

    public boolean removeChatId(Long chatId) {
        return chatIds.remove(chatId);
    }

    public ListLinksResponse getLinks(Long chatId) {
        logger.log(Level.INFO, "Getting links for " + chatId + "\n " + trackedLinks.toString());
        return trackedLinks.getOrDefault(chatId, new ListLinksResponse());
    }

    public void addLink(Long chatId, AddLinkRequest link) {
        ListLinksResponse response = trackedLinks.get(chatId);
        LinkResponse linkResponse = new LinkResponse(chatId, link.link());
        if (response == null) {
            Set<LinkResponse> lR = new HashSet<>();
            lR.add(linkResponse);
            trackedLinks.put(chatId, new ListLinksResponse(lR));
            return;
        }
        response.addLink(linkResponse);
        trackedLinks.put(chatId, response);
    }

    public boolean removeLink(Long chatId, RemoveLinkRequest removeLinkRequest) {
        ListLinksResponse links = trackedLinks.get(chatId);
        if (links.deleteLink(removeLinkRequest.link())) {
            trackedLinks.put(chatId, links);
            return true;
        }
        return false;
    }

    @Scheduled(fixedRate = 60000)
    private boolean isUpdated(String link) {
        if (isGithubLink(link)) {
            if (!lastUpdated.containsKey(link)) {
                lastUpdated.put(link, githubClient.updateTime(link));
                return false;
            }
            return lastUpdated.get(link).isBefore(githubClient.updateTime(link));
        } else if (isStackoverflow(link)) {
            logger.info("Stack overflow пока не поддерживается");
            throw new UnsupportedOperationException("StackOverflow links not supported yet");
        }
        logger.info("Неверная ссылка");
        throw new UnsupportedOperationException("Unsupported link type: " + link);
    }

    private boolean isGithubLink(String link) {
        return link.startsWith("https://github.com/");
    }

    private boolean isStackoverflow(String link) {
        return link.startsWith("https://stackoverflow.com/");
    }

}
