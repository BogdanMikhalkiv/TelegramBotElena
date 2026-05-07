package com.example.telegrambotelena;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.longpolling.interfaces.LongPollingUpdateConsumer;
import org.telegram.telegrambots.longpolling.starter.SpringLongPollingBot;

@Component
@AllArgsConstructor
public class MyTelegramBot  implements SpringLongPollingBot {

    private final UpdateConsumer updateConsumer;
    @Override
    public String getBotToken() {
        return "8611415969:AAG62hlCyuW6PZqlPaa0IsE9zfI08igNS8c";
    }

    @Override
    public LongPollingUpdateConsumer getUpdatesConsumer() {
        return updateConsumer;
    }
}
