package backend.academy.scrapper.service.clients;

import backend.academy.scrapper.service.Answer;
import java.time.LocalDateTime;

public interface ClientAnswer {
    boolean hasUpdates();
    String getAuthor();
    String getUrl();
    String getDescription();
    LocalDateTime getUpdatedAt();
    Answer getAnswer();
}
