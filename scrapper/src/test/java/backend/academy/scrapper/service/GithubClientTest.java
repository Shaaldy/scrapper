package backend.academy.scrapper.service;

import backend.academy.scrapper.ScrapperConfig;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GithubClientTest {

    private final String testRepo = "https://github.com/user/repo";
    @Mock
    private RestTemplate restTemplate;
    @Mock
    private ScrapperConfig scrapperConfig;
    @InjectMocks
    private GithubClient githubClient;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
        when(scrapperConfig.restTemplate()).thenReturn(restTemplate);
        String testToken = "ghp_testToken123";
        when(scrapperConfig.githubToken()).thenReturn(testToken);

        githubClient = new GithubClient(scrapperConfig);
    }

    @Test
    void testGetUpdatedAtSuccess() {
        String isoTime = "2024-12-01T15:42:00Z";
        Map<String, Object> responseBody = Map.of("updated_at", isoTime);

        ResponseEntity<Map> mockResponse = new ResponseEntity<>(responseBody, HttpStatus.OK);

        String expectedUrl = "https://api.github.com/repos/user/repo";
        when(restTemplate.exchange(eq(expectedUrl), eq(HttpMethod.GET), ArgumentMatchers.<HttpEntity<Map>>any(), eq(Map.class))).thenReturn(mockResponse);

        LocalDateTime updated = githubClient.getUpdatedAt(testRepo);

        Instant instant = Instant.parse(isoTime);
        LocalDateTime expected = LocalDateTime.ofInstant(instant, ZoneOffset.UTC);

        assertThat(updated).isEqualTo(expected);

        verify(restTemplate).exchange(eq(expectedUrl), eq(HttpMethod.GET), any(), eq(Map.class));
    }


    @Test
    void testGetUpdatedAtThrowsExceptionIfNoUpdatedAt() {
        Map<String, Object> responseBody = Map.of();

        ResponseEntity<Map> mockResponse = new ResponseEntity<>(responseBody, HttpStatus.OK);

        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(), eq(Map.class))).thenReturn(mockResponse);

        Exception ex = assertThrows(IllegalArgumentException.class, () -> githubClient.getUpdatedAt(testRepo));
        assertThat(ex.getMessage()).contains("Missing 'updated_at'");
    }

}
