package backend.academy.api;

import java.util.ArrayList;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class BotAPI {

    @GetMapping("/updates")
    public ArrayList<ApiErrorResponse> updates(LinkUpdate linkUpdate) {
        return new ArrayList<>();
    }

}
