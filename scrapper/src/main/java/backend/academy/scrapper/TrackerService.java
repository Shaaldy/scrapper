package backend.academy.scrapper;


import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
@EnableScheduling
public class TrackerService {
    Logger logger = LoggerFactory.getLogger(TrackerService.class);
    protected static DateTimeFormatter ISO_INSTANT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");

    GithubClient githubClient;
    RestTemplate restTemplate;

    Map<Long, ListLinksResponse> trackedLinks = new HashMap<>();
    Set<Long> chatIds = new HashSet<>();
    Map<String, Integer> linkCount = new HashMap<>();
    Map<String, LocalDateTime> lastUpdated = new HashMap<>();

    @Autowired
    public TrackerService(GithubClient githubClient,  RestTemplate restTemplate) {
        this.githubClient = githubClient;
        this.restTemplate = restTemplate;
    }

    public boolean addChatId(Long chatId) {
        chatIds.add(chatId);
        return true;
    }

    public boolean removeChatId(Long chatId) {
        trackedLinks.remove(chatId);
        for (LinkResponse linkResponse : trackedLinks.get(chatId).links) {
            deleteFromSet(linkResponse);
        }
        return chatIds.remove(chatId);
    }

    public ListLinksResponse getLinks(Long chatId) {
        logger.info("Getting links for {}\n {}", chatId, trackedLinks.toString());
        return trackedLinks.getOrDefault(chatId, new ListLinksResponse());
    }

    public void addLink(Long chatId, AddLinkRequest addLinkRequest) {
        ListLinksResponse response = trackedLinks.get(chatId);
        String link = addLinkRequest.getLink();
        LinkResponse linkResponse = new LinkResponse(chatId, link);
        if(linkCount.containsKey(link)) {
            linkCount.replace(link, linkCount.get(link) + 1);
        }
        else{
            linkCount.put(link, 1);
        }
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
        String link = removeLinkRequest.getLink();
        if (links.deleteLink(link)) {
            deleteFromSet(removeLinkRequest);
            trackedLinks.put(chatId, links);
            return true;
        }
        return false;
    }


    private void deleteFromSet(ILinked iLinked){
        String url = iLinked.getLink();
        if (linkCount.containsKey(url)){
            if (linkCount.get(url) <= 1){
                linkCount.remove(url);
            }
            else{
                linkCount.replace(url, linkCount.get(url) - 1);
            }
        }
    }

    private boolean isUpdated(String link) {
        if (isGithubLink(link)) {
            if (!lastUpdated.containsKey(link)) {
                lastUpdated.put(link, githubClient.updateTime(link));
                logger.info("Первый запрос по ссылке {}", link);
                return false;
            }
            LocalDateTime time1 = lastUpdated.get(link);
            LocalDateTime time2 = githubClient.updateTime(link);
            logger.info("Вребя обновления репозитория в БД - {}\n Время обновления репозитория после HTTP-запроса - {}", time1, time2);
            return time1.isBefore(time2);
        } else if (isStackoverflow(link)) {
            logger.info("Stack overflow пока не поддерживается");
            throw new UnsupportedOperationException("StackOverflow links not supported yet");
        }
        logger.info("Неверная ссылка");
        throw new UnsupportedOperationException("Unsupported link type: " + link);
    }
//
//    @Scheduled(fixedRate = 6000)
//    private void sendHTTPResponse(Long chatId){
//        try{
//            for(Map.Entry<String, Integer> entry : linkCount.entrySet()){}
//
//            HttpEntity<Void> httpEntity = new HttpEntity<>(headers);
//        }
//        catch (Exception e){
//
//        }
//    }

    private boolean isGithubLink(String link) {
        return link.startsWith("https://github.com/");
    }

    private boolean isStackoverflow(String link) {
        return link.startsWith("https://stackoverflow.com/");
    }

}
