package backend.academy.api;


import backend.academy.bot.BotService;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("api/bot")
public class BotAPI {

    BotService botService;

    @Autowired
    public BotAPI(BotService botService) {
        this.botService = botService;
    }

    @PostMapping("/updates")
    public ResponseEntity<?> updates(@RequestHeader String url, @RequestHeader String description, @RequestBody Set<Long> tgChatIds) {
        for (Long chatId : tgChatIds) {
            if (!botService.sendUpdate(chatId, url)) {
                ApiErrorResponse apiErrorResponse = new ApiErrorResponse("Не корректные параметры запроса", "400", "IllegalArgumentException", "ID чата должен быть положительным числом", null);
                return ResponseEntity.badRequest().body(apiErrorResponse);
            }
        }
        return ResponseEntity.ok("Удалось отправить обновление");
    }

}
