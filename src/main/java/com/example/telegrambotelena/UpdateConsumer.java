package com.example.telegrambotelena;

import org.springframework.stereotype.Component;
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer;
import org.telegram.telegrambots.meta.api.objects.Update;

@Component
public class UpdateConsumer  implements LongPollingSingleThreadUpdateConsumer {
    @Override
    public void consume(Update update) {
        System.out.println(update.getMessage().getText() + " от " + update.getMessage().getChatId());
    }
}
