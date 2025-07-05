package backend.academy.bot;

import backend.academy.api.ApiErrorResponse;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.UpdatesListener;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.SendMessage;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    Logger logger = LoggerFactory.getLogger(BotService.class);
    TelegramBot telegramBot;
    RestTemplate restTemplate;
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
            if (text.equals("/start")) registerChat(chatId);
            else {
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
                case "/delete" -> deleteChat(chatId);
                default -> sendMessage("Данная команда не поддерживается, список команда: /help", chatId);
            }
        } else if (state == State.TRACKED) {
            handleTracked(text, chatId);
        } else if (state == State.UNTRACKED) {
            handleUntracked(text, chatId);
        }
    }


    private void sendMessage(String text, long chatId) {
        telegramBot.execute(new SendMessage(chatId, text));
    }


    private boolean isInvalidURL(String url) {
        try {
            URI uri = new URI(url);
            return uri.getScheme() == null;  // URLs must have a scheme
        } catch (URISyntaxException | IllegalArgumentException e) {
            return true;
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
                logger.info("Пользователь с Id {} зарегестрирован", chatId);
            } else {
                sendMessage("Регистрация неудачная", chatId);
            }

        } catch (Exception e) {
            sendMessage("Ошибка при регистрации. Возможно, вы уже зарегистрированы. Если нет, попробуйте позже. ", chatId);
            logger.error(e.getMessage());
        }
    }

    /**
     * Удаляет Id пользователя в системе Scrapper.
     * Отправляет запрос на удаление записи о чате.
     *
     * @param chatId идентификатор чата пользователя в Telegram
     */
    private void deleteChat(long chatId) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Tg-chat-id", String.valueOf(chatId));
            HttpEntity<String> entity = new HttpEntity<>(headers);
            ApiErrorResponse apiErrorResponse = restTemplate.exchange(scrapperApiUrl + "/tg-chat/{chatId}", HttpMethod.DELETE, entity, ApiErrorResponse.class, chatId).getBody();
            assert apiErrorResponse != null;
            logger.info(apiErrorResponse.toString());
            if (apiErrorResponse.code().equals("200")) {
                sendMessage("ID пользователя удалено из хранилища, для начала диалога напишите /start", chatId);
            } else {
                sendMessage("Не удалось удалить пользователя, возможно он уже удален", chatId);
            }
        } catch (Exception e) {
            sendMessage("Ошибка в работе БД", chatId);
            logger.error(e.getMessage());
        } finally {
            userStates.remove(chatId);
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
            logger.info("Запрос на получение ссылок удачен для пользователя с ID {}", chatId);
            if (links != null && !links.isEmpty()) {
                sendMessage("Отслеживаемые ссылки:\n" + String.join("\n", links), chatId);
            } else {
                sendMessage("Нет отслеживаемых ссылок", chatId);
            }
        } catch (Exception e) {
            sendMessage("Ошибка в работе БД", chatId);
            logger.error(e.getMessage());
        }
    }


    /**
     * Обработка ссылки, которую пользователь отправил в чат
     * Отправление запроса на добавление ссылки в хранилище (в будущем БД)
     *
     * @param url    отслеживаемая ссылка
     * @param chatId идентификатор чата пользователя в Telegram
     */
    private void handleTracked(String url, long chatId) {
        if (isInvalidURL(url)) {
            logger.error("Некорректная ссылка {}", url);
            sendMessage("Введите пожалуйста ссылка", chatId);
            return;
        }
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Tg-chat-id", String.valueOf(chatId));
            HttpEntity<String> entity = new HttpEntity<>(url, headers);
            restTemplate.exchange(scrapperApiUrl + "/links", HttpMethod.POST, entity, Void.class);
            userStates.put(chatId, State.CONTINUE);
            sendMessage("Ссылка успешно добавлена", chatId);
            logger.info("Ссылка {} для пользователя {} успешно добавлена", url, chatId);
        } catch (Exception e) {
            sendMessage("Ошибка в работде БД", chatId);
            logger.error("Ошибка в системе:\n Ссылка {} пользователя {}\n{}", url, chatId, e.getMessage());
        }
    }

    /**
     * Обработка удаления ссылки из отслеживаемых
     * Отправление запроса на удаление ссылки из хранилища
     *
     * @param url    отслеживаемая ссылка
     * @param chatId идентификатор чата пользователя в Telegram
     */
    private void handleUntracked(String url, long chatId) {
        if (isInvalidURL(url)) {
            logger.error("Некорректная ссылка {}", url);
            sendMessage("Введите пожалуйста ссылка", chatId);
            return;
        }
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Tg-chat-id", String.valueOf(chatId));
            HttpEntity<String> entity = new HttpEntity<>(url, headers);
            ResponseEntity<ApiErrorResponse> response = restTemplate.exchange(scrapperApiUrl + "/links", HttpMethod.DELETE, entity, ApiErrorResponse.class);
            ApiErrorResponse apiErrorResponse = response.getBody();
            logger.info("apiErrorResponse: {}", apiErrorResponse);
            if (apiErrorResponse.code().equals("200")) {
                userStates.put(chatId, State.CONTINUE);
                sendMessage("Ссылка теперь не отслеживается", chatId);
            } else {
                sendMessage("Такой ссылки нет", chatId);
            }
        } catch (Exception e) {
            sendMessage("Ошибка в работе БД", chatId);
            logger.error(e.getMessage());
        }
    }

}


