package backend.academy.scrapper.service.clients.github;

import backend.academy.scrapper.service.Answer;
import lombok.Getter;
import java.time.LocalDateTime;

@Getter
public class PRAnswer extends GithubAnswer {
    public PRAnswer(LocalDateTime updatedAt, String login, String url, String description, Answer answer, boolean hasUpdate) {
        super(updatedAt, login, url, description, answer,  hasUpdate);
        if (answer != Answer.PR){
            throw new IllegalArgumentException("Not a PRAnswer");
        }
    }
}
