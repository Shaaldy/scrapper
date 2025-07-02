package backend.academy.scrapper.api;


import backend.academy.scrapper.TrackerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ScrapperAPI {

    TrackerService trackerService;

    @Autowired
    public ScrapperAPI(TrackerService trackerService) {
        this.trackerService = trackerService;
    }

    @PostMapping("/tg-chat/{id}")
    public ResponseEntity<ApiErrorResponse> registrationTgChat(@PathVariable Long id) {
        if (trackerService.addChatId(id)) {
            ApiErrorResponse apiErrorResponse = new ApiErrorResponse("success", "200", "-", "-", null);
            return new ResponseEntity<>(apiErrorResponse, HttpStatus.OK);
        }
        ApiErrorResponse apiErrorResponse = new ApiErrorResponse("fall", "400", "-", "-", null);
        return new ResponseEntity<>(apiErrorResponse, HttpStatus.NOT_FOUND);
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
    public ResponseEntity<ListLinksResponse> getLinks(@PathVariable Long id) {
        ListLinksResponse setLinks = trackerService.getLinks(id);
        if (setLinks.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        return new ResponseEntity<>(setLinks, HttpStatus.OK);
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
