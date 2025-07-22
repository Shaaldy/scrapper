package backend.academy.bot;

import backend.academy.api.BotAPI;
import backend.academy.bot.BotService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Set;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(BotAPI.class)
class BotAPITest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private BotService botService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void testUpdatesSuccess() throws Exception {
        Set<Long> chatIds = Set.of(111L, 222L);
        for (Long id : chatIds) {
            when(botService.sendUpdate(eq(id), eq("https://repo"))).thenReturn(true);
        }

        mockMvc.perform(post("/api/bot/updates")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(chatIds))
                .header("url", "https://repo")
                .header("description", "some update"))
            .andExpect(status().isOk())
            .andExpect(content().string("Удалось отправить обновление"));
    }

    @Test
    void testUpdatesFailure() throws Exception {
        Set<Long> chatIds = Set.of(111L, 222L);
        when(botService.sendUpdate(eq(111L), eq("https://repo"))).thenReturn(false);

        mockMvc.perform(post("/api/bot/updates")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(chatIds))
                .header("url", "https://repo")
                .header("description", "some update"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.description").value("Не корректные параметры запроса"))
            .andExpect(jsonPath("$.code").value("400"))
            .andExpect(jsonPath("$.exceptionName").value("IllegalArgumentException"))
            .andExpect(jsonPath("$.exceptionMessage").value("ID чата должен быть положительным числом"));
    }
}
