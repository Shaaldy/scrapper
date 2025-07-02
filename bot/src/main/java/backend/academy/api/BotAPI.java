package backend.academy.api;

import backend.academy.bot.BotService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.ArrayList;

@RestController
public class BotAPI {

    @GetMapping("/updates")
    public ArrayList<ApiErrorResponse> updates(LinkUpdate linkUpdate) {
        return new ArrayList<>();
    }

}
