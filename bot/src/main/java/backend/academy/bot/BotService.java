package backend.academy.bot;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.UpdatesListener;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.SendMessage;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;


enum State {START, TRACKED, UNTRACKED, CONTINUE}

@Service
public class BotService {

    private final String resourceUrl = "http://localhost:8080";
    TelegramBot telegramBot;
    State state;
    RestTemplate restTemplate;
    Map<Long, Set<String>> trackedLinks = new HashMap<>();

    @Autowired
    public BotService(BotConfig botConfig) {
        this.telegramBot = new TelegramBot(botConfig.telegramToken());
        this.state = State.START;
        this.restTemplate = botConfig.restTemplate();
        telegramBot.setUpdatesListener(updates -> {
            for (Update update : updates) {
                handleUpdate(update);
            }
            return UpdatesListener.CONFIRMED_UPDATES_ALL;
        });
    }

    protected void handleUpdate(Update update) {
        System.out.println("Текущее состояние: " + state);
        long chatId = update.message().chat().id();
        String text = update.message().text();
        if (state == State.START) {
            if (text.equals("/start")) {
                String url = resourceUrl + "/tg-chat/";
                Long obj = chatId;
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                HttpEntity<Long> entity = new HttpEntity<>(obj, headers);

                ResponseEntity<Long> response = restTemplate.exchange(url, HttpMethod.POST, entity, Long.class);

                if (response.getStatusCode().is2xxSuccessful()) {
                    sendMessage("Вы успешно зарегестрированы", chatId);
                    state = State.CONTINUE;
                } else {
                    sendMessage("Регистрация не удалась из-за сбоев работы", chatId);
                }
            } else {
                sendMessage("Вы не зарегестрированы, напишите /start", chatId);
            }
        } else if (state == State.CONTINUE) {
            switch (text) {
                case "/start" -> sendMessage("Вы уже зарегестрированы", chatId);
                case "/help" -> sendMessage("Команда помощи", chatId);
                case "/list" -> sendMessage("Пока не реализовано", chatId);
                case "/track" -> {
                    sendMessage("Укажите ссылку для отслеживания: ", chatId);
                    state = State.TRACKED;
                }
                case "/untrack" -> {
                    sendMessage("Укажите ссылку, которую нужно удалить: ", chatId);
                    state = State.UNTRACKED;
                }
            }
        } else if (state == State.TRACKED) {
            handleTracked(text, chatId);
        } else if (state == State.UNTRACKED) {
            handleUntracked(text, chatId);
        }
    }

    private void handleTracked(String url, long chatId) {
        if (isValidURL(url)) {
            Set<String> trackedUrls = trackedLinks.get(chatId);
            if (trackedUrls == null) {
                trackedUrls = new HashSet<>();
            }
            trackedUrls.add(url);
            trackedLinks.put(chatId, trackedUrls);
            sendMessage("Ссылка добавлена", chatId);
        }
        state = State.CONTINUE;
    }


    private void handleUntracked(String url, long chatId) {
        Set<String> trackedUrls = trackedLinks.get(chatId);
        if (trackedUrls.remove(url)) {
            sendMessage("Ссылка " + url + " удалена", chatId);
        } else {
            sendMessage("Такой ссылки нет", chatId);
        }
        state = State.CONTINUE;
    }

    private void sendMessage(String text, long chatId) {
        telegramBot.execute(new SendMessage(chatId, text));
    }


    boolean isValidURL(String url) {
        try {
            URI uri = new URI(url);
            return uri.getScheme() != null;  // URLs must have a scheme
        } catch (URISyntaxException | IllegalArgumentException e) {
            return false;
        }
    }
}


