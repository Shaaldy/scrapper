package backend.academy.scrapper;


import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import io.swagger.v3.oas.annotations.headers.Header;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController()
@RequestMapping("api/scrapper")
public class ScrapperAPI {
    Logger logger = Logger.getLogger(ScrapperAPI.class.getName());
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
        Set<LinkResponse> setLinks = trackerService.getLinks(id).getLinks();
        Set<String> urls = setLinks.stream().map(LinkResponse::url).collect(Collectors.toSet());
        return ResponseEntity.ok(Map.of("links", urls, "size", setLinks.size()));
    }

    @PostMapping("/links")
    public ResponseEntity<?> addLink(@RequestHeader("Tg-chat-id") Long id, @RequestBody String addLinkRequest) {
        try{
            trackerService.addLink(id, new AddLinkRequest(addLinkRequest));
            return ResponseEntity.ok("Ссылка добавлена");
        }
        catch(Exception e) {
            ApiErrorResponse apiErrorResponse = new ApiErrorResponse("fall", "400", "-", "-", null);
            return ResponseEntity.badRequest().body(apiErrorResponse);
        }
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
