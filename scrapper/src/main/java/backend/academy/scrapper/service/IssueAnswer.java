package backend.academy.scrapper.service;

import lombok.Getter;
import java.time.LocalDateTime;

@Getter
public class IssueAnswer extends GithubBase {
    public IssueAnswer(LocalDateTime updatedAt, String login, String link, String description){
        super(updatedAt, login, link, description);
    }

}
