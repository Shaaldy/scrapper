package backend.academy.scrapper.bd;

import backend.academy.scrapper.api.ListLinksResponse;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.Set;

@Repository
public interface StorageProvider {
    boolean addUser(Long chatId);
    boolean removeUser(Long chatId);
    ListLinksResponse getLinks(Long chatId);
    void addLink(Long chatId, String url);
    boolean removeLink(Long chatId, String url);
    Set<Long> getSubscribers(String url);
    Set<String> getAllTrackedLinks();

    boolean updateTime(String url, LocalDateTime time);
}
