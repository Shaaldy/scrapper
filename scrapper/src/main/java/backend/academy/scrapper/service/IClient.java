package backend.academy.scrapper.service;

import java.time.LocalDateTime;
import java.util.function.Function;

public interface IClient {
    LocalDateTime getUpdatedAt(String link);
    UpdateInfo checkForUpdates(String link);
}
