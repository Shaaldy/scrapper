package backend.academy.scrapper.service.github;


import backend.academy.scrapper.service.Answer;
import backend.academy.scrapper.service.ClientAnswer;
import backend.academy.scrapper.service.Updatable;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;


@Getter
public class GithubAnswer implements ClientAnswer, Updatable {

    private LocalDateTime updatedAt;
    private String author;
    private String url;
    private String description;
    private Answer answer;
    @Setter
    private boolean hasUpdate;


    public GithubAnswer(LocalDateTime updatedAt, String author, String url, String description, Answer answer) {
        this.updatedAt = updatedAt;
        this.author = author;
        this.url = url;
        this.description = description;
        if (answer.isGITHUB()) {
            this.answer = answer;
        }
    }

    public GithubAnswer(LocalDateTime updatedAt, Answer answer) {
        this.updatedAt = updatedAt;
        if (answer.isGITHUB()) {
            this.answer = answer;
        }
    }

    public GithubAnswer(boolean hasUpdate) {
        this.hasUpdate = hasUpdate;
    }

    @Override
    public boolean hasUpdates() {
        return hasUpdate;
    }

}
