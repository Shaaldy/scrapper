package backend.academy.bot;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.Chat;
import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.SendMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BotServiceTest {

    private final long chatId = 123456789L;
    @Mock
    private TelegramBot telegramBot;
    @Mock
    private RestTemplate restTemplate;
    @InjectMocks
    private BotService botService;
    @Captor
    private ArgumentCaptor<SendMessage> sendMessageCaptor;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        botService = new BotService(telegramBot, restTemplate);
    }

    private Update mockUpdate(String text) {
        Chat chat = mock(Chat.class);
        when(chat.id()).thenReturn(chatId);

        Message message = mock(Message.class);
        when(message.chat()).thenReturn(chat);
        when(message.text()).thenReturn(text);

        Update update = mock(Update.class);
        when(update.message()).thenReturn(message);
        return update;
    }

    @Test
    void testStartCommandRegistersUserSuccessfully() {
        Update update = mockUpdate("/start");

        when(restTemplate.postForEntity(eq("http://localhost:8081/api/scrapper/tg-chat/{chatId}"), isNull(), eq(Void.class), eq(chatId))).thenReturn(new ResponseEntity<>(HttpStatus.OK));

        botService.scrapperApiUrl = "http://localhost:8081/api/scrapper";
        botService.handleUpdate(update);

        verify(telegramBot).execute(sendMessageCaptor.capture());
        String response = sendMessageCaptor.getValue().getParameters().get("text").toString();
        assert response.equals("Регистрация успешная");
    }


    @Test
    void testSendUpdateFailure() {
        doThrow(new RuntimeException("fail")).when(telegramBot).execute(any(SendMessage.class));

        boolean result = botService.sendUpdate(chatId, "https://fail");

        assert !result;
        verify(telegramBot).execute(any(SendMessage.class));
    }

    @Test
    void testHelpCommand() {
        botService.userStates.put(chatId, State.CONTINUE);
        Update update = mockUpdate("/help");

        botService.handleUpdate(update);

        verify(telegramBot).execute(sendMessageCaptor.capture());
        assert sendMessageCaptor.getValue().getParameters().get("text").equals("Команда помощи");
    }

    @Test
    void testUnknownCommand() {
        botService.userStates.put(chatId, State.CONTINUE);
        Update update = mockUpdate("/something");

        botService.handleUpdate(update);

        verify(telegramBot).execute(sendMessageCaptor.capture());
        String response = sendMessageCaptor.getValue().getParameters().get("text").toString();
        assert response.contains("Данная команда не поддерживается");
    }

    @Test
    void testTrackCommandFlow() {
        botService.userStates.put(chatId, State.CONTINUE);
        Update update = mockUpdate("/track");

        botService.handleUpdate(update);

        verify(telegramBot).execute(sendMessageCaptor.capture());
        assert sendMessageCaptor.getValue().getParameters().get("text").equals("Укажите ссылку для отслеживания: ");
        assert botService.userStates.get(chatId) == State.TRACKED;
    }

    @Test
    void testTrackWithInvalidUrl() {
        botService.userStates.put(chatId, State.TRACKED);
        Update update = mockUpdate("not-a-url");

        botService.handleUpdate(update);

        verify(telegramBot).execute(sendMessageCaptor.capture());
        String text = sendMessageCaptor.getValue().getParameters().get("text").toString();
        assert text.contains("Введите пожалуйста ссылку");
    }
}
