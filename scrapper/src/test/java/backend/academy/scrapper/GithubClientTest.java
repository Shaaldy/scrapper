package backend.academy.scrapper;

import backend.academy.scrapper.service.GithubClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

import static java.time.format.DateTimeFormatter.ISO_INSTANT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class GithubClientTest {

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private ScrapperConfig scrapperConfig;

    @InjectMocks
    private GithubClient githubClient;

    private final String testToken = "ghp_testToken123";
    private final String testRepo = "https://github.com/user/repo";
    private final String expectedUrl = "https://api.github.com/repos/user/repo";

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
        when(scrapperConfig.restTemplate()).thenReturn(restTemplate);
        when(scrapperConfig.githubToken()).thenReturn(testToken);

        githubClient = new GithubClient(scrapperConfig);
    }

    @Test
    void testGetUpdatedAtSuccess() {
        String isoTime = "2024-12-01T15:42:00Z";
        Map<String, Object> responseBody = Map.of("updated_at", isoTime);

        ResponseEntity<Map> mockResponse = new ResponseEntity<>(responseBody, HttpStatus.OK);

        when(restTemplate.exchange(
            eq(expectedUrl),
            eq(HttpMethod.GET),
            ArgumentMatchers.<HttpEntity<Map>>any(),
            eq(Map.class)
        )).thenReturn(mockResponse);

        LocalDateTime updated = githubClient.getUpdatedAt(testRepo);

        assertThat(updated).isEqualTo(LocalDateTime.parse(isoTime, ISO_INSTANT));

        verify(restTemplate).exchange(eq(expectedUrl), eq(HttpMethod.GET), any(), eq(Map.class));
    }

    @Test
    void testGetUpdatedAtThrowsExceptionIfNoUpdatedAt() {
        Map<String, Object> responseBody = Map.of(); // No "updated_at"

        ResponseEntity<Map> mockResponse = new ResponseEntity<>(responseBody, HttpStatus.OK);

        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(), eq(Map.class)))
            .thenReturn(mockResponse);

        try {
            githubClient.getUpdatedAt(testRepo);
            assert false : "Expected NullPointerException or parsing error";
        } catch (Exception e) {
            assertThat(e).isInstanceOf(NullPointerException.class)
                .isInstanceOf(IllegalArgumentException.class);
        }
    }
}
