package backend.academy.scrapper.service.clients.stackoverflow;

import backend.academy.scrapper.service.Answer;
import lombok.Getter;
import java.time.LocalDateTime;

@Getter
public class SORequestAnswer extends SOAnswer {
    public SORequestAnswer(LocalDateTime updatedAt, String author, String url, String title, String description, Answer answer, boolean hasUpdate) {
        super(updatedAt, author, url, title, description, answer, hasUpdate);
        if (answer != Answer.SO_REQUEST){
            throw new IllegalArgumentException("Not SO Request");
        }
    }
}
