package backend.academy.scrapper.bd;

import backend.academy.scrapper.api.LinkResponse;
import backend.academy.scrapper.api.ListLinksResponse;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

@Repository
@Profile("in-memory")
public class Storage implements StorageProvider {

    private final Map<Long, ListLinksResponse> userLinks = new HashMap<>();
    private final Map<String, Set<Long>> linkSubscribers = new HashMap<>();
    private final Map<String, LocalDateTime> lastLinkUpdate = new HashMap<>();


    @Override
    public boolean addUser(Long chatId) {
        return userLinks.putIfAbsent(chatId, new ListLinksResponse()) == null;
    }

    @Override
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

    @Override
    public ListLinksResponse getLinks(Long chatId) {
        return userLinks.getOrDefault(chatId, new ListLinksResponse());
    }

    @Override
    public void addLink(Long chatId, String url) {
        userLinks.computeIfAbsent(chatId, _ -> new ListLinksResponse());

        LinkResponse linkResponse = new LinkResponse(chatId, url);
        userLinks.get(chatId).addLink(linkResponse);

        linkSubscribers.computeIfAbsent(url, _ -> new HashSet<>()).add(chatId);
    }

    @Override
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

    @Override
    public Set<Long> getSubscribers(String url) {
        return linkSubscribers.getOrDefault(url, Set.of());
    }

    @Override
    public Set<String> getAllTrackedLinks() {
        return linkSubscribers.keySet();
    }

    @Override
    public boolean updateTime(String url, LocalDateTime time) {
        if (!linkSubscribers.containsKey(url)) {
            throw new IllegalStateException("User does not exist");
        }
        if (lastLinkUpdate.containsKey(url)) {
            if (lastLinkUpdate.get(url).isBefore(time)) {
                lastLinkUpdate.put(url, time);
                return true;
            }
            return false;
        } else {
            lastLinkUpdate.put(url, time);
            return true;
        }
    }
}

