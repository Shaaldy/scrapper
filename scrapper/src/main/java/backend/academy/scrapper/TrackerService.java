package backend.academy.scrapper;

import backend.academy.scrapper.api.AddLinkRequest;
import backend.academy.scrapper.api.LinkResponse;
import backend.academy.scrapper.api.ListLinksResponse;
import backend.academy.scrapper.api.RemoveLinkRequest;
import org.springframework.stereotype.Service;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Service
public class TrackerService {
    Map<Long, ListLinksResponse> trackedLinks = new HashMap<>();
    Set<Long> chatIds;

    public boolean addChatId(Long chatId) {
        chatIds.add(chatId);
        return true;
    }

    public boolean removeChatId(Long chatId) {
        return chatIds.remove(chatId);
    }

    public ListLinksResponse getLinks(Long chatId) {
        return trackedLinks.get(chatId);
    }

    public boolean addLink(Long chatId, AddLinkRequest link) {
        ListLinksResponse response = trackedLinks.get(chatId);
        LinkResponse linkResponse = new LinkResponse(chatId, link.link(), link.filters(), link.tags());
        if (response == null) {
            Set<LinkResponse> lR =  new HashSet<>();
            lR.add(linkResponse);
            response = new ListLinksResponse(lR);
            return true;
        }

        return response.addLink(linkResponse);

    }

    public boolean removeLink(Long chatId, RemoveLinkRequest removeLinkRequest) {
        ListLinksResponse links = trackedLinks.get(chatId);
        return links.deleteLink(removeLinkRequest.link());
    }
}
