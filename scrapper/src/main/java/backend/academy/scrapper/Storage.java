package backend.academy.scrapper;

import backend.academy.scrapper.api.LinkResponse;
import backend.academy.scrapper.api.ListLinksResponse;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class Storage {

    private final Map<Long, ListLinksResponse> userLinks = new HashMap<>();
    private final Map<String, Set<Long>> linkSubscribers = new HashMap<>();
    private final Map<String, LocalDateTime> lastUpdated = new HashMap<>();

    public boolean addUser(Long chatId) {
        return userLinks.putIfAbsent(chatId, new ListLinksResponse()) == null;
    }

    public boolean removeUser(Long chatId) {
        ListLinksResponse links = userLinks.remove(chatId);
        if (links != null) {
            for (LinkResponse link : links.getLinks()) {
                removeSubscription(link.getLink(), chatId);
            }
            return true;
        }
        return false;
    }

    public ListLinksResponse getLinks(Long chatId) {
        return userLinks.getOrDefault(chatId, new ListLinksResponse());
    }

    public void addLink(Long chatId, String url) {
        userLinks.computeIfAbsent(chatId, _ -> new ListLinksResponse());

        LinkResponse linkResponse = new LinkResponse(chatId, url);
        userLinks.get(chatId).addLink(linkResponse);

        linkSubscribers.computeIfAbsent(url, _ -> new HashSet<>()).add(chatId);
    }

    public boolean removeLink(Long chatId, String url) {
        ListLinksResponse links = userLinks.get(chatId);
        if (links != null && links.deleteLink(url)) {
            removeSubscription(url, chatId);
            return true;
        }
        return false;
    }

    private void removeSubscription(String url, Long chatId) {
        Set<Long> subs = linkSubscribers.get(url);
        if (subs != null) {
            subs.remove(chatId);
            if (subs.isEmpty()) {
                linkSubscribers.remove(url);
            }
        }
    }

    public Set<Long> getSubscribers(String url) {
        return linkSubscribers.getOrDefault(url, Set.of());
    }

    public Set<String> getAllTrackedLinks() {
        return linkSubscribers.keySet();
    }

    public boolean isUpdated(String link, LocalDateTime newTime) {
        LocalDateTime prev = lastUpdated.get(link);
        if (prev == null || prev.isBefore(newTime)) {
            lastUpdated.put(link, newTime);
            return prev != null;
        }
        return false;
    }

}

