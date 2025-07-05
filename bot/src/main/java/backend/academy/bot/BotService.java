package backend.academy.bot;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.UpdatesListener;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.SendMessage;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;


enum State {START, TRACKED, UNTRACKED, CONTINUE}

@Service
public class BotService {

    TelegramBot telegramBot;
    RestTemplate restTemplate;
    Map<Long, Set<String>> trackedLinks = new HashMap<>();
    Map<Long, State> userStates = new HashMap<>();
    @Value("${app.scrapperApiUrl}")
    private String scrapperApiUrl;

    @Autowired
    public BotService(BotConfig botConfig) {
        this.telegramBot = new TelegramBot(botConfig.telegramToken());
        this.restTemplate = botConfig.restTemplate();
        telegramBot.setUpdatesListener(updates -> {
            for (Update update : updates) {
                handleUpdate(update);
            }
            return UpdatesListener.CONFIRMED_UPDATES_ALL;
        });
    }

    protected void handleUpdate(Update update) {
        if (update.message() == null) {
            return;
        }
        long chatId = update.message().chat().id();
        String text = update.message().text();
        State state = userStates.getOrDefault(chatId, State.START);
        System.out.println("Текущее состояние: " + state);
        if (state == State.START) {
            if (text.equals("/start"))
                registerChat(chatId);
            else{
                sendMessage("Вы еще не зарегестрированы", chatId);
            }
        } else if (state == State.CONTINUE) {
            switch (text) {
                case "/start" -> sendMessage("Вы уже зарегестрированы", chatId);
                case "/help" -> sendMessage("Команда помощи", chatId);
                case "/list" -> getList(chatId);
                case "/track" -> {
                    sendMessage("Укажите ссылку для отслеживания: ", chatId);
                    userStates.put(chatId, State.TRACKED);
                }
                case "/untrack" -> {
                    sendMessage("Укажите ссылку, которую нужно удалить: ", chatId);
                    userStates.put(chatId, State.UNTRACKED);
                }
                default -> sendMessage("Данная команда не поддерживается, список команда: /help", chatId);
            }
        } else if (state == State.TRACKED) {
            handleTracked(text, chatId);
        } else if (state == State.UNTRACKED) {
            handleUntracked(text, chatId);
        }
    }



    private void handleUntracked(String url, long chatId) {
        Set<String> trackedUrls = trackedLinks.get(chatId);
        if (trackedUrls.remove(url)) {
            sendMessage("Ссылка " + url + " удалена", chatId);
        } else {
            sendMessage("Такой ссылки нет", chatId);
        }
        userStates.put(chatId, State.CONTINUE);
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

    /**
     * Регистрирует пользователя в системе Scrapper для возможности отслеживания ссылок.
     * Отправляет запрос на создание записи о чате.
     *
     * @param chatId идентификатор чата пользователя в Telegram
     */
    private void registerChat(long chatId) {
        try {
            ResponseEntity<Void> response = restTemplate.postForEntity(scrapperApiUrl + "/tg-chat/{chatId}", null, Void.class, chatId);
            if (response.getStatusCode().is2xxSuccessful()) {
                userStates.put(chatId, State.CONTINUE);
                sendMessage("Регистрация успешная", chatId);
            } else {
                sendMessage("Регистрация неудачная", chatId);
            }

        } catch (Exception e) {
            sendMessage("Ошибка при регистрации. Возможно, вы уже зарегистрированы. Если нет, попробуйте позже. ", chatId);
        }
    }

    /**
     * Выдает пользователю список ссылок, которые он отслеживает
     * Отправляет запрос на выдачу ссылок
     *
     * @param chatId идентификатор чата пользователя в Telegram
     */
    private void getList(long chatId) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Tg-chat-id", String.valueOf(chatId));
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<Map> response = restTemplate.exchange(scrapperApiUrl + "/links", HttpMethod.GET, entity, Map.class);
            Map objectsMap = response.getBody();
            List<String> links = (List<String>) objectsMap.get("links");
            if (links != null && !links.isEmpty()) {
                sendMessage("Отслеживаемые ссылки: " + String.join("\n", links), chatId);
            } else {
                sendMessage("Нет отслеживаемых ссылок", chatId);
            }
        } catch (Exception e) {
            sendMessage("Ошибка в работе БД", chatId);
        }
    }



    private void handleTracked(String url, long chatId) {
        if (!isValidURL(url)) {
            sendMessage("Некорректная ссылка, введите заново", chatId);
        }
        else{

            try{
                HttpHeaders headers = new HttpHeaders();
                headers.set("Tg-chat-id", String.valueOf(chatId));
                HttpEntity<String> entity = new HttpEntity<>(url, headers);
                ResponseEntity<Void> response = restTemplate.exchange(scrapperApiUrl + "/links", HttpMethod.POST, entity, Void.class);

                if (response.getStatusCode().is2xxSuccessful()) {
                    userStates.put(chatId, State.CONTINUE);
                    sendMessage("Ссылка успешно добавлена", chatId);
                } else {
                    sendMessage("Неудалось добавить, возможно уже она есть в списке", chatId);
                }
            }
            catch(Exception e){
                sendMessage("Ошибка в работде БД", chatId);
            }
        }

    }
}


