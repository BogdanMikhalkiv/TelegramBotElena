package com.example.telegrambotelena;

import com.example.telegrambotelena.GoogleAPIConfig.GoogleSheetsLiveTest;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;

import java.io.IOException;
import java.security.GeneralSecurityException;

@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class})
public class TelegramBotElenaApplication {

    public static void main(String[] args) {
        SpringApplication.run(TelegramBotElenaApplication.class, args);
        //test

    }

}
