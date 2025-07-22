package backend.academy.scrapper.api;

import backend.academy.scrapper.service.TrackerService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Set;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ScrapperAPI.class)
public class ScrapperAPITest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TrackerService trackerService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void registerChat_success() throws Exception {
        when(trackerService.addChatId(123L)).thenReturn(true);

        mockMvc.perform(post("/api/scrapper/tg-chat/123"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("Чат зарегистрирован.")));
    }

    @Test
    void registerChat_invalidId() throws Exception {
        mockMvc.perform(post("/api/scrapper/tg-chat/-1"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.exceptionMessage").value("ID чата должен быть положительным числом"));
    }

    @Test
    void registerChat_alreadyExists() throws Exception {
        when(trackerService.addChatId(123L)).thenReturn(false);

        mockMvc.perform(post("/api/scrapper/tg-chat/123"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.exceptionMessage").value("Чат с таким ID уже существует"));
    }

    @Test
    void deleteTgChat_success() throws Exception {
        when(trackerService.removeChatId(123L)).thenReturn(true);

        mockMvc.perform(delete("/api/scrapper/tg-chat/123")
                .header("Tg-chat-id", 123))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.description").value("ID чата удален"));
    }

    @Test
    void deleteTgChat_fail() throws Exception {
        when(trackerService.removeChatId(123L)).thenReturn(false);

        mockMvc.perform(delete("/api/scrapper/tg-chat/123")
                .header("Tg-chat-id", 123))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.description").value("Не удалось удалить"));
    }

    @Test
    void getLinks_returnsLinks() throws Exception {
        Set<LinkResponse> links = Set.of(
            new LinkResponse(123L, "https://github.com/test")
        );
        when(trackerService.getLinks(123L)).thenReturn(new ListLinksResponse(links));

        mockMvc.perform(get("/api/scrapper/links")
                .header("Tg-chat-id", 123))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.links[0]").value("https://github.com/test"))
            .andExpect(jsonPath("$.size").value(1));
    }

    @Test
    void addLink_success() throws Exception {
        mockMvc.perform(post("/api/scrapper/links")
                .header("Tg-chat-id", 123)
                .contentType(MediaType.APPLICATION_JSON)
                .content("\"https://github.com/test\""))
            .andExpect(status().isOk())
            .andExpect(content().string("Ссылка добавлена"));
    }

    @Test
    void deleteLink_success() throws Exception {
        when(trackerService.removeLink(eq(123L), any(RemoveLinkRequest.class))).thenReturn(true);


        mockMvc.perform(delete("/api/scrapper/links")
                .header("Tg-chat-id", 123)
                .contentType(MediaType.APPLICATION_JSON)
                .content("\"https://github.com/test\""))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.description").value("success"));
    }

    @Test
    void deleteLink_notFound() throws Exception {
        when(trackerService.removeLink(123L, new RemoveLinkRequest("https://github.com/test"))).thenReturn(false);

        mockMvc.perform(delete("/api/scrapper/links")
                .header("Tg-chat-id", 123)
                .contentType(MediaType.APPLICATION_JSON)
                .content("\"https://github.com/test\""))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.description").value("У пользователя с таким ID нет этой ссылки"));
    }
}
