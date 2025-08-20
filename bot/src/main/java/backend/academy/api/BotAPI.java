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
    public ResponseEntity<?> updates(@RequestHeader("url") String url,
                                     @RequestHeader("update_type") String Update_Type,
                                     @RequestHeader("updated_at") String updated_at,
                                     @RequestHeader (value = "description", required = false) String description,
                                     @RequestHeader (value = "author", required = false) String author,
                                     @RequestBody Set<Long> tgChatIds) {
        for (Long chatId : tgChatIds) {
            GHAns update = new GHAns(Update_Type, url, description, author, updated_at);

            if (!botService.sendUpdate(chatId, update)) {
                ApiErrorResponse apiErrorResponse = new ApiErrorResponse("Не корректные параметры запроса", "400", "IllegalArgumentException", "ID чата должен быть положительным числом", null);
                return ResponseEntity.badRequest().body(apiErrorResponse);
            }
        }
        return ResponseEntity.ok("Удалось отправить обновление");
    }

}
