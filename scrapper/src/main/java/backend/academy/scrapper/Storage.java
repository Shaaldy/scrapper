package backend.academy.scrapper;

import backend.academy.scrapper.api.LinkResponse;
import backend.academy.scrapper.api.ListLinksResponse;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import backend.academy.scrapper.service.clients.Updatable;
import backend.academy.scrapper.service.clients.github.IssueAnswer;
import backend.academy.scrapper.service.clients.github.PRAnswer;
import backend.academy.scrapper.service.clients.github.RepoAnswer;
import backend.academy.scrapper.service.clients.stackoverflow.SOCommentAnswer;
import backend.academy.scrapper.service.clients.stackoverflow.SORequestAnswer;
import org.springframework.stereotype.Component;

@Component
public class Storage {

    private final Map<Long, ListLinksResponse> userLinks = new HashMap<>();
    private final Map<String, Set<Long>> linkSubscribers = new HashMap<>();
    private final Map<String, SOCommentAnswer> lastSOComment = new HashMap<>();
    private final Map<String, SORequestAnswer> lastSORequest = new HashMap<>();
    private final Map<String, PRAnswer> lastPRAnswer = new HashMap<>();
    private final Map<String, IssueAnswer>  lastIssueAnswer = new HashMap<>();
    private final Map<String, RepoAnswer> lastRepoAnswer = new HashMap<>();


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


    public boolean setPR(String link, PRAnswer prAnswer) {
        return setAnswer(link, prAnswer, lastPRAnswer);
    }

    public boolean setIssue(String link, IssueAnswer issueAnswer) {
        return setAnswer(link, issueAnswer, lastIssueAnswer);
    }

    public boolean setRepo(String link, RepoAnswer repoAnswer) {
        return setAnswer(link, repoAnswer, lastRepoAnswer);
    }

    public boolean setSOR(String link, SORequestAnswer sor) {
        return setAnswer(link, sor, lastSORequest);
    }

    public boolean setSOC(String link, SOCommentAnswer soc) {
        return setAnswer(link, soc, lastSOComment);
    }

    private <T extends Updatable> boolean setAnswer(
        String link,
        T newAnswer,
        Map<String, T> storage
    ) {
        T previous = storage.get(link);
        LocalDateTime prevTime = previous != null ? previous.getUpdatedAt() : null;

        if (prevTime == null || prevTime.isBefore(newAnswer.getUpdatedAt())) {
            storage.put(link, newAnswer);
            return prevTime != null; // true = обновление, false = первый раз
        }
        return false;
    }

}

