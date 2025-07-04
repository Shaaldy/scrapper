package backend.academy.scrapper;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class TrackerService {
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
        return trackedLinks.getOrDefault(chatId, new ListLinksResponse());
    }

    public boolean addLink(Long chatId, AddLinkRequest link) {
        ListLinksResponse response = trackedLinks.get(chatId);
        LinkResponse linkResponse = new LinkResponse(chatId, link.link());
        if (response == null) {
            Set<LinkResponse> lR = new HashSet<>();
            lR.add(linkResponse);
            return true;
        }
        return response.addLink(linkResponse);

    }

    public boolean removeLink(Long chatId, RemoveLinkRequest removeLinkRequest) {
        ListLinksResponse links = trackedLinks.get(chatId);
        return links.deleteLink(removeLinkRequest.link());
    }
}
