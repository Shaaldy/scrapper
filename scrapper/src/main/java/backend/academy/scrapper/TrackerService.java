package backend.academy.scrapper;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.springframework.stereotype.Service;

@Service
public class TrackerService {
    Logger logger = Logger.getLogger(TrackerService.class.getName());
    Map<Long, ListLinksResponse> trackedLinks = new HashMap<>();
    Set<Long> chatIds = new HashSet<>();

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
}
