package backend.academy.scrapper.service.clients.github;

import backend.academy.scrapper.service.Answer;
import lombok.Getter;
import java.time.LocalDateTime;

@Getter
public class RepoAnswer extends GithubAnswer {
    RepoAnswer(LocalDateTime localDateTime, Answer answer, boolean hasUpdate) {
        super(localDateTime, answer, hasUpdate);
        if (answer != Answer.REPO){
            throw new IllegalArgumentException("Not REPO request");
        }
    }
}
