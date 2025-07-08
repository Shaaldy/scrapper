package backend.academy;

import jakarta.validation.constraints.NotEmpty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.client.RestTemplate;

@Validated
@ConfigurationProperties(prefix = "app", ignoreUnknownFields = false)
public record BotConfig(@NotEmpty String telegramToken, @NotEmpty String scrapperApiUrl, @NotEmpty String botApiUrl) {
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
