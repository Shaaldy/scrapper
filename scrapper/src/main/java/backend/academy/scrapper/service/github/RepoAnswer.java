package backend.academy.scrapper.service.github;

import backend.academy.scrapper.service.Answer;
import java.time.LocalDateTime;
import lombok.Getter;

@Getter
public class RepoAnswer extends GithubAnswer {
    RepoAnswer(LocalDateTime localDateTime, Answer answer) {
        super(localDateTime, answer);
        if (answer != Answer.REPO) {
            throw new IllegalArgumentException("Not REPO request");
        }
    }
}
