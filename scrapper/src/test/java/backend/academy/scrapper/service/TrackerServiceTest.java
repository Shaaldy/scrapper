package backend.academy.scrapper.service;

import backend.academy.scrapper.api.AddLinkRequest;
import backend.academy.scrapper.api.RemoveLinkRequest;
import backend.academy.scrapper.api.ListLinksResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

public class TrackerServiceTest {

    @Mock
    GithubClient githubClient;

    @Mock
    SOClient soClient;

    @Mock
    RestTemplate restTemplate;

    @InjectMocks
    TrackerService trackerService;

    final Long chatId = 1L;
    final String githubLink = "https://github.com/owner/repo";
    final String soLink = "https://ru.stackoverflow.com/questions/123456/title";
    final String unknownLink = "https://example.com/test";

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        trackerService = new TrackerService(githubClient, soClient, restTemplate);
    }

    @Test
    void testAddChatId() {
        boolean result = trackerService.addChatId(chatId);
        assertThat(result).isTrue();
    }

    @Test
    void testAddAndGetLinks() {
        trackerService.addChatId(chatId);
        trackerService.addLink(chatId, new AddLinkRequest(githubLink));

        ListLinksResponse links = trackerService.getLinks(chatId);
        assertThat(links.getLinks()).hasSize(1);
        assertThat(links.getLinks().iterator().next().getLink()).isEqualTo(githubLink);
    }

    @Test
    void testRemoveLinkSuccessfully() {
        trackerService.addChatId(chatId);
        trackerService.addLink(chatId, new AddLinkRequest(githubLink));

        boolean removed = trackerService.removeLink(chatId, new RemoveLinkRequest(githubLink));
        assertThat(removed).isTrue();

        ListLinksResponse after = trackerService.getLinks(chatId);
        assertThat(after.getLinks()).isEmpty();
    }

    @Test
    void testRemoveLinkFailsIfNotFound() {
        trackerService.addChatId(chatId);
        boolean removed = trackerService.removeLink(chatId, new RemoveLinkRequest(githubLink));
        assertThat(removed).isFalse();
    }

    @Test
    void testSendHTTPResponseForGithubUpdated() {
        trackerService.addChatId(chatId);
        trackerService.addLink(chatId, new AddLinkRequest(githubLink));

        LocalDateTime oldTime = LocalDateTime.of(2023, 1, 1, 0, 0);
        LocalDateTime newTime = LocalDateTime.of(2024, 1, 1, 0, 0);

        // 1-й вызов: запоминаем
        when(githubClient.getUpdatedAt(githubLink)).thenReturn(oldTime, newTime);

        // Мокаем успешный POST в бот
        when(restTemplate.exchange(
            contains("/updates"),
            eq(HttpMethod.POST),
            any(HttpEntity.class),
            eq(Void.class)))
            .thenReturn(ResponseEntity.ok().build());

        // 2 вызова подряд
        trackerService.sendHTTPResponse(); // установит oldTime
        trackerService.sendHTTPResponse(); // сравнит и пошлёт update

        verify(restTemplate, times(1)).exchange(
            contains("/updates"),
            eq(HttpMethod.POST),
            any(HttpEntity.class),
            eq(Void.class));
    }

    @Test
    void testSendHTTPResponseForStackoverflowUpdated() {
        trackerService.addChatId(chatId);
        trackerService.addLink(chatId, new AddLinkRequest(soLink));

        LocalDateTime oldTime = LocalDateTime.of(2023, 1, 1, 0, 0);
        LocalDateTime newTime = LocalDateTime.of(2024, 1, 1, 0, 0);

        when(soClient.getUpdatedAt(soLink)).thenReturn(oldTime, newTime);

        when(restTemplate.exchange(
            contains("/updates"),
            eq(HttpMethod.POST),
            any(HttpEntity.class),
            eq(Void.class)))
            .thenReturn(ResponseEntity.ok().build());

        trackerService.sendHTTPResponse();
        trackerService.sendHTTPResponse();

        verify(restTemplate, times(1)).exchange(
            contains("/updates"),
            eq(HttpMethod.POST),
            any(HttpEntity.class),
            eq(Void.class));
    }

    @Test
    void testSendHTTPResponseSkipsUnsupportedLink() {
        trackerService.addChatId(chatId);
        trackerService.addLink(chatId, new AddLinkRequest(unknownLink));

        // Ошибка не должна вызывать исключение
        assertThatCode(() -> trackerService.sendHTTPResponse())
            .doesNotThrowAnyException();
    }

    @Test
    void testSendHTTPResponseHandlesRestTemplateExceptionGracefully() {
        trackerService.addChatId(chatId);
        trackerService.addLink(chatId, new AddLinkRequest(githubLink));

        LocalDateTime oldTime = LocalDateTime.of(2023, 1, 1, 0, 0);
        LocalDateTime newTime = LocalDateTime.of(2024, 1, 1, 0, 0);
        when(githubClient.getUpdatedAt(githubLink)).thenReturn(oldTime, newTime);

        when(restTemplate.exchange(
            contains("/updates"),
            eq(HttpMethod.POST),
            any(HttpEntity.class),
            eq(Void.class)))
            .thenThrow(new RuntimeException("Simulated network error"));

        assertThatCode(() -> {
            trackerService.sendHTTPResponse();
            trackerService.sendHTTPResponse();
        }).doesNotThrowAnyException(); // исключение должно быть перехвачено
    }
}
