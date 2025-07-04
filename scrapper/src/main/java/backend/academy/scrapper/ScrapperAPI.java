package backend.academy.scrapper;


import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController()
@RequestMapping("api/scrapper")
public class ScrapperAPI {

    TrackerService trackerService;

    @Autowired
    public ScrapperAPI(TrackerService trackerService) {
        this.trackerService = trackerService;
    }

    /**
     * Регистрирует новый Telegram-чат для использования сервиса.
     *
     * @param id ID Telegram-чата, передается как путь переменной.
     * @return Подтверждение регистрации или сообщение об ошибке, если чат уже зарегистрирован.
     */
    @PostMapping("/tg-chat/{id}")
    public ResponseEntity<?> registerChat(@PathVariable Long id) {
        if (id == null || id <= 0) {
            return ResponseEntity.badRequest().body(new ApiErrorResponse("Некорректные параметры запроса", "400", "IllegalArgumentException", "ID чата должен быть положительным числом", null));
        }
        boolean success = trackerService.addChatId(id);
        if (!success) {
            return ResponseEntity.badRequest().body(new ApiErrorResponse("Чат уже зарегистрирован", "400", "ChatAlreadyExistsException", "Чат с таким ID уже существует", null));
        }
        return ResponseEntity.ok("Чат зарегистрирован.");
    }

    @DeleteMapping("/tg-chat/{id}")
    public ResponseEntity<ApiErrorResponse> deleteTgChat(@PathVariable Long id) {
        if (trackerService.removeChatId(id)) {
            ApiErrorResponse apiErrorResponse = new ApiErrorResponse("success", "200", "-", "-", null);
            return new ResponseEntity<>(apiErrorResponse, HttpStatus.OK);
        }
        ApiErrorResponse apiErrorResponse = new ApiErrorResponse("fall", "400", "-", "-", null);
        return new ResponseEntity<>(apiErrorResponse, HttpStatus.NOT_FOUND);
    }

    @GetMapping("/links")
    public ResponseEntity<?> getLinks(@RequestHeader("Tg-chat-id") Long id) {
        ListLinksResponse setLinks = trackerService.getLinks(id);
        return ResponseEntity.ok(Map.of("links", setLinks.getLinks(), "size", setLinks.getSize()));
    }

    @PostMapping("/links")
    public ResponseEntity<ApiErrorResponse> addLink(@PathVariable Long id, AddLinkRequest addLinkRequest) {
        if (trackerService.addLink(id, addLinkRequest)) {
            ApiErrorResponse apiErrorResponse = new ApiErrorResponse("success", "200", "-", "-", null);
            return new ResponseEntity<>(apiErrorResponse, HttpStatus.OK);
        }
        ApiErrorResponse apiErrorResponse = new ApiErrorResponse("fall", "400", "-", "-", null);
        return new ResponseEntity<>(apiErrorResponse, HttpStatus.NOT_FOUND);
    }

    @DeleteMapping("/links")
    public ResponseEntity<ApiErrorResponse> deleteLink(@PathVariable Long id, RemoveLinkRequest removeLinkRequest) {
        if (trackerService.removeLink(id, removeLinkRequest)) {
            ApiErrorResponse apiErrorResponse = new ApiErrorResponse("success", "200", "-", "-", null);
            return new ResponseEntity<>(apiErrorResponse, HttpStatus.OK);
        }
        ApiErrorResponse apiErrorResponse = new ApiErrorResponse("fall", "400", "-", "-", null);
        return new ResponseEntity<>(apiErrorResponse, HttpStatus.NOT_FOUND);
    }
}
