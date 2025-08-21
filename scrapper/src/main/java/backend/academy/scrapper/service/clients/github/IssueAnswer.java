package backend.academy.scrapper.service.clients.github;

import backend.academy.scrapper.service.Answer;
import lombok.Getter;
import java.time.LocalDateTime;

@Getter
public class IssueAnswer extends GithubAnswer {
    public IssueAnswer(LocalDateTime updatedAt, String login, String url, String description, Answer answer, boolean hasUpdate) {
        super(updatedAt, login, url, description, answer,  hasUpdate);
        if (answer != Answer.ISSUE){
            throw new IllegalArgumentException("Not a IssueAnswer");
        }
    }
}
