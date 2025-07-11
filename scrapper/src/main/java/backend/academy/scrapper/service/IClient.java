package backend.academy.scrapper.service;

import java.time.LocalDateTime;

public interface IClient {
    LocalDateTime getUpdatedAt(String link);

}
