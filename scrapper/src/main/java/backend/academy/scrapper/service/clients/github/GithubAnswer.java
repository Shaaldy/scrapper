package backend.academy.scrapper.service.clients.github;


import backend.academy.scrapper.service.Answer;
import backend.academy.scrapper.service.clients.ClientAnswer;
import backend.academy.scrapper.service.clients.Updatable;
import lombok.Getter;
import java.time.LocalDateTime;


@Getter
public class GithubAnswer implements ClientAnswer, Updatable {

    private LocalDateTime updatedAt;
    private String author;
    private String url;
    private String description;
    private Answer answer;
    private boolean hasUpdate;


    public GithubAnswer(LocalDateTime updatedAt, String author, String url, String description, Answer answer, boolean hasUpdate) {
        this.updatedAt = updatedAt;
        this.author = author;
        this.url = url;
        this.description = description;
        if (answer.isGITHUB()) {
            this.answer = answer;
        }
        this.hasUpdate = hasUpdate;
    }

    public GithubAnswer(LocalDateTime updatedAt, Answer answer, boolean hasUpdate) {
        this.updatedAt = updatedAt;
        if (answer.isGITHUB()) {
            this.answer = answer;
        }
        this.hasUpdate = hasUpdate;
    }

    public GithubAnswer(boolean hasUpdate) {
        this.hasUpdate = hasUpdate;
    }

    @Override
    public boolean hasUpdates() {
        return hasUpdate;
    }

}
