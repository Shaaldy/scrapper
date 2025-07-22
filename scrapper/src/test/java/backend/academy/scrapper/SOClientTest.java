package backend.academy.scrapper;


import backend.academy.scrapper.ScrapperConfig.StackOverflowCredentials;
import backend.academy.scrapper.service.SOClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.InvalidUrlException;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class SOClientTest {

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private ScrapperConfig scrapperConfig;

    @Mock
    private StackOverflowCredentials soCredentials;

    @InjectMocks
    private SOClient soClient;

    private final String validLink = "https://ru.stackoverflow.com/questions/12345678/example-question/";
    private final String invalidLink = "https://ru.stackoverflow.com/blabla/123456/";

    private final String expectedUrl = "https://api.stackexchange.com/2.3/questions/12345678/answers?site=ru.stackoverflow&sort=activity";

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
        when(scrapperConfig.restTemplate()).thenReturn(restTemplate);
        when(scrapperConfig.stackOverflow()).thenReturn(soCredentials);
        when(soCredentials.accessToken()).thenReturn("dummy-token");

        soClient = new SOClient(scrapperConfig);
    }

    @Test
    void testGetUpdatedAtSuccess() {
        int lastActivityEpoch = 1710000000;
        Map<String, Object> item = Map.of("last_activity_date", lastActivityEpoch);
        Map<String, Object> response = Map.of("items", List.of(item));

        ResponseEntity<Map> responseEntity = new ResponseEntity<>(response, HttpStatus.OK);
        when(restTemplate.exchange(eq(expectedUrl), eq(HttpMethod.GET), any(HttpEntity.class), eq(Map.class)))
            .thenReturn(responseEntity);

        LocalDateTime result = soClient.getUpdatedAt(validLink);
        LocalDateTime expected = LocalDateTime.ofInstant(Instant.ofEpochSecond(lastActivityEpoch), ZoneOffset.UTC);

        assertThat(result).isEqualTo(expected);
    }

    @Test
    void testGetUpdatedAtThrowsForInvalidLink() {
        InvalidUrlException ex = assertThrows(InvalidUrlException.class, () -> soClient.getUpdatedAt(invalidLink));
        assertThat(ex.getMessage()).isEqualTo("Invalid URL");
    }

    @Test
    void testGetUpdatedAtThrowsIfNoItems() {
        Map<String, Object> response = Map.of("items", List.of());

        ResponseEntity<Map> responseEntity = new ResponseEntity<>(response, HttpStatus.OK);
        when(restTemplate.exchange(anyString(), any(), any(), eq(Map.class)))
            .thenReturn(responseEntity);

        RuntimeException ex = assertThrows(RuntimeException.class, () -> soClient.getUpdatedAt(validLink));
        assertThat(ex.getMessage()).startsWith("Error getting last activity on link");
    }

    @Test
    void testGetUpdatedAtThrowsIfNullResponse() {
        ResponseEntity<Map> responseEntity = new ResponseEntity<>(null, HttpStatus.OK);
        when(restTemplate.exchange(anyString(), any(), any(), eq(Map.class)))
            .thenReturn(responseEntity);

        RuntimeException ex = assertThrows(RuntimeException.class, () -> soClient.getUpdatedAt(validLink));
        assertThat(ex.getMessage()).startsWith("Error getting last activity on link");
    }

    @Test
    void testGetUpdatedAtThrowsIfBodyIsNull() {
        ResponseEntity<Map> response = new ResponseEntity<>(null, HttpStatus.OK);
        when(restTemplate.exchange(anyString(), any(), any(), eq(Map.class)))
            .thenReturn(response);

        RuntimeException ex = assertThrows(RuntimeException.class, () -> soClient.getUpdatedAt(validLink));
        assertThat(ex.getMessage()).contains("no response body");
    }

    @Test
    void testGetUpdatedAtThrowsIfNoItemsField() {
        Map<String, Object> body = Map.of(); // no "items"
        ResponseEntity<Map> response = new ResponseEntity<>(body, HttpStatus.OK);
        when(restTemplate.exchange(anyString(), any(), any(), eq(Map.class)))
            .thenReturn(response);

        RuntimeException ex = assertThrows(RuntimeException.class, () -> soClient.getUpdatedAt(validLink));
        assertThat(ex.getMessage()).contains("missing 'items'");
    }

    @Test
    void testGetUpdatedAtThrowsIfItemsEmpty() {
        Map<String, Object> body = Map.of("items", List.of());
        ResponseEntity<Map> response = new ResponseEntity<>(body, HttpStatus.OK);
        when(restTemplate.exchange(anyString(), any(), any(), eq(Map.class)))
            .thenReturn(response);

        RuntimeException ex = assertThrows(RuntimeException.class, () -> soClient.getUpdatedAt(validLink));
        assertThat(ex.getMessage()).contains("no items");
    }

    @Test
    void testGetUpdatedAtThrowsIfNoLastActivityDate() {
        Map<String, Object> item = Map.of();
        Map<String, Object> body = Map.of("items", List.of(item));
        ResponseEntity<Map> response = new ResponseEntity<>(body, HttpStatus.OK);
        when(restTemplate.exchange(anyString(), any(), any(), eq(Map.class)))
            .thenReturn(response);

        RuntimeException ex = assertThrows(RuntimeException.class, () -> soClient.getUpdatedAt(validLink));
        assertThat(ex.getMessage()).contains("missing 'last_activity_date'");
    }

    @Test
    void testGetUpdatedAtThrowsIfInvalidUrl() {
        String badLink = "https://stackoverflow.com/not-a-question/";
        InvalidUrlException ex = assertThrows(InvalidUrlException.class, () -> soClient.getUpdatedAt(badLink));
        assertThat(ex.getMessage()).isEqualTo("Invalid URL");
    }
}
