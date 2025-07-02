package backend.academy.bot;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.UpdatesListener;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.SendMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Service
class BotService {

    TelegramBot telegramBot;
    State state;
    Map<Long, Set<String>> trackedLinks = new HashMap<>();

    @Autowired
    public BotService(BotConfig botConfig) {
        this.telegramBot = new TelegramBot(botConfig.telegramToken());
        this.state = State.START;
        update();
    }

    protected void update(){
        telegramBot.setUpdatesListener(updates -> {
            for (Update update : updates) {
                handleUpdate(update);
            }
            return UpdatesListener.CONFIRMED_UPDATES_ALL;
        }, e -> {
            if (e.response() != null) {
                // got bad response from telegram
                e.response().errorCode();
                e.response().description();
            } else {
                // probably network error
                e.printStackTrace();
            }
        });
    }

    protected void handleUpdate(Update update) {
        long chatId = update.message().chat().id();
        String text = update.message().text();
        if (state == State.START) {
            if (text.equals("/start")) {
                sendMessage("Добро пожаловать, регистрация пройдена", chatId);
                System.out.println("Пришло сообщение");
                state = State.CONTINUE;
            } else {
                sendMessage("Вы не зарегестрированы", chatId);
            }
        } else if (state == State.CONTINUE) {
            switch (text) {
                case "/start" -> sendMessage("Вы уже зарегестрированы", chatId);
                case "/help" -> sendMessage("Команда помощи", chatId);
                case "/list" -> sendMessage("Пока не реализовано", chatId);
                case "/track" -> state = State.TRACKED;
                case "/untrack" -> state = State.UNTRACKED;
            }
        } else if (state == State.TRACKED) {
            handleTracked(text, chatId);
        } else if (state == State.UNTRACKED) {
            handleUntracked(text, chatId);
        }
    }

    private void handleTracked(String url, long chatId){
        if (isValidURL(url)){
            Set<String> trackedUrls = trackedLinks.get(chatId);
            trackedUrls.add(url);
            trackedLinks.put(chatId,trackedUrls);
            sendMessage("Ссылка добавлена", chatId);
        }
        state = State.CONTINUE;
    }


    private void handleUntracked(String url, long chatId){
        Set<String> trackedUrls = trackedLinks.get(chatId);
        if (trackedUrls.remove(url)){
            sendMessage("Ссылка " + url + " удалена", chatId);
        }
        else{
            sendMessage("Такой ссылки нет", chatId);
        }
        state = State.CONTINUE;
    }

    private void sendMessage(String text, long chatId){
        telegramBot.execute(new SendMessage(chatId,text));
    }


    boolean isValidURL(String url) {
        try {
            URI uri = new URI(url);
            if (uri.getScheme() == null) {
                return false;  // URLs must have a scheme
            }
            return true;
        } catch (URISyntaxException | IllegalArgumentException e) {
            return false;
        }
    }
}


