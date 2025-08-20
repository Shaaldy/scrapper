package backend.academy.scrapper.service;

import lombok.Getter;
import java.time.LocalDateTime;

@Getter
public class PRAnswer extends GithubBase {
    public PRAnswer(LocalDateTime updatedAt, String login, String link, String description){
        super(updatedAt, login, link, description); // ← Правильный порядок
    }
}
