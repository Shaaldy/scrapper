package backend.academy.scrapper.service.github;

import backend.academy.scrapper.service.Answer;
import java.time.LocalDateTime;
import lombok.Getter;

@Getter
public class IssueAnswer extends GithubAnswer {
    public IssueAnswer(LocalDateTime updatedAt, String login, String url, String description, Answer answer) {
        super(updatedAt, login, url, description, answer);
        if (answer != Answer.ISSUE) {
            throw new IllegalArgumentException("Not a IssueAnswer");
        }
    }
}
