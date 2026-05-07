package com.example.telegrambotelena;

import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.util.List;

@Component
public class UpdateConsumer  implements LongPollingSingleThreadUpdateConsumer {
    private final TelegramClient telegramClient;

    @Value("${bot.token}")
    private  String botToken;

    public UpdateConsumer(@Value("${bot.token}") String botToken ) {
        this.telegramClient = new OkHttpTelegramClient(botToken);
    }

    @SneakyThrows
    @Override
    public void consume(Update update) {


        if (update.hasMessage()) {
            if (update.getMessage().getText().equals("/start")) {
                sendMainMenu(update.getMessage().getChatId());

            } else {

                SendMessage sendMessage = SendMessage.builder()
                        .text("привет , я тебя не понимаю")
                        .chatId(update.getMessage().getChatId())
                        .build();

                telegramClient.execute(sendMessage);
            }


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
    }
}
