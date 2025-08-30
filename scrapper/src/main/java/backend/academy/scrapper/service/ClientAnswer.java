package backend.academy.scrapper.service;

import java.time.LocalDateTime;

public interface ClientAnswer {
    boolean hasUpdates();

    String getAuthor();

    String getUrl();

    String getDescription();

    LocalDateTime getUpdatedAt();

    Answer getAnswer();
}
