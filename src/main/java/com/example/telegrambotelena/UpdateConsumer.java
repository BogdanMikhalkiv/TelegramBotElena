package com.example.telegrambotelena;

import com.example.telegrambotelena.BotStageEnum.BotStage;
import com.example.telegrambotelena.Model.Client;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class UpdateConsumer  implements LongPollingSingleThreadUpdateConsumer {
    private final TelegramClient telegramClient;
    private Client client;
    private boolean questionnaireMode = false;
    private final Map<Long,Client> clientMap = new HashMap<>();

    @Value("${bot.token}")
    private  String botToken;
//    private UserSessionHandler userSessionHandler;

    public UpdateConsumer(@Value("${bot.token}") String botToken , Client client) {
        this.telegramClient = new OkHttpTelegramClient(botToken);
        this.client = client;
    }

    @SneakyThrows
    @Override
    public void consume(Update update) {
        Client session = clientMap.computeIfAbsent(
                getChatID(update),
                k -> Client.builder().botStage(BotStage.IDLE).build());

        if (update.hasMessage()) {
            if (update.getMessage().getText().equals("/start")) {
                sendMainMenu(getChatID(update));
            } else {
               // sendMsg(update.getMessage().getChatId(), "привет , я тебя не понимаю" );
                questionnaireFormMethod(getChatID(update),session, update);
            }
        } else if (update.hasCallbackQuery()) {
            handleCallbackQuery(update.getCallbackQuery(), update, client);
        }

    }
    @SneakyThrows
    public void sendMsg(Long chatID, String textMsg) {
        SendMessage sendMessage = SendMessage.builder()
                .text(textMsg)
                .chatId(chatID)
                .build();

        telegramClient.execute(sendMessage);
    }

    public Long getChatID(Update update) {
        return update.getMessage().getChatId();
    }

    private void handleCallbackQuery(CallbackQuery callbackQuery, Update update, Client client) {
        var data = callbackQuery.getData();
        var chatID = callbackQuery.getFrom().getId();
        var user = callbackQuery.getFrom();

        switch (data) {
            case "questionnaire":
                client.setBotStage(BotStage.WAITING_NAME);
                sendMsg(chatID,"Введите имя");
        }
    }

    private void questionnaireFormMethod(Long chatID, Client client, Update update) {
        sendMsg(chatID, "Введите фамилию:");
        String text = update.getMessage().getText();
        switch (client.getBotStage()) {

            case WAITING_NAME:
                client.setName(text);
                client.setBotStage(BotStage.WAITING_SURNAME);
                break;

            case WAITING_SURNAME:
                client.setSurname(text);
                client.setBotStage(BotStage.IDLE);
                break;

        }

    }


    @SneakyThrows
    private void sendMainMenu(Long chatId) {
        SendMessage message = SendMessage.builder()
                .text("Добро пожаловать в ТГ Бот Елены, выберите следующие действия: ")
                .chatId(chatId)
                .build();

        var button1 = InlineKeyboardButton.builder()
                .text("Заполнить анкету")
                .callbackData("questionnaire")
                .build();


        List<InlineKeyboardRow> inlineKeyboardRows = List.of(new InlineKeyboardRow( button1));

        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup(inlineKeyboardRows);

        message.setReplyMarkup(inlineKeyboardMarkup);

        telegramClient.execute(message);

        //sendMsg(chatId, message.getText());
    }
}
