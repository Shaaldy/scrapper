package backend.academy.scrapper.service.stackoverflow;

import backend.academy.scrapper.service.Answer;
import backend.academy.scrapper.service.ClientAnswer;
import backend.academy.scrapper.service.Updatable;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter
public class SOAnswer implements ClientAnswer, Updatable {
    private LocalDateTime updatedAt;
    private String author;
    private String url;
    private String title;
    private String description;
    private Answer answer;
    @Setter
    private boolean hasUpdate;

    public SOAnswer(LocalDateTime updatedAt, String author, String url, String title, String description, Answer answer) {
        this.updatedAt = updatedAt;
        this.author = author;
        this.url = url;
        this.title = title;
        this.description = description;
        if (answer.isSO()) {
            this.answer = answer;
        }
    }

    public SOAnswer(LocalDateTime updatedAt, Answer answer) {
        this.updatedAt = updatedAt;
        if (answer.isSO()) {
            this.answer = answer;
        }
    }

    public SOAnswer(boolean hasUpdate) {
        this.hasUpdate = hasUpdate;
    }

    public boolean hasUpdates() {
        return hasUpdate;
    }
}
