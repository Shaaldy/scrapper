package backend.academy.scrapper.service.stackoverflow;

import backend.academy.scrapper.service.Answer;
import java.time.LocalDateTime;
import lombok.Getter;

@Getter
public class SORequestAnswer extends SOAnswer {
    public SORequestAnswer(LocalDateTime updatedAt, String author, String url, String title, String description, Answer answer) {
        super(updatedAt, author, url, title, description, answer);
        if (answer != Answer.SO_REQUEST) {
            throw new IllegalArgumentException("Not SO Request");
        }
    }
}
