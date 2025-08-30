package backend.academy.scrapper.service.github;

import backend.academy.scrapper.service.Answer;
import java.time.LocalDateTime;
import lombok.Getter;

@Getter
public class PRAnswer extends GithubAnswer {
    public PRAnswer(LocalDateTime updatedAt, String login, String url, String description, Answer answer) {
        super(updatedAt, login, url, description, answer);
        if (answer != Answer.PR) {
            throw new IllegalArgumentException("Not a PRAnswer");
        }
    }
}
