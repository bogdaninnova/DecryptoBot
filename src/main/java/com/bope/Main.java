package com.bope;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.telegram.telegrambots.ApiContextInitializer;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

@SpringBootApplication
public class Main {

    public static AnnotationConfigApplicationContext ctx;
    public static void main(String[] args) {

        ApiContextInitializer.init();
        TelegramBotsApi telegramBotsApi = new TelegramBotsApi();
        ctx = new AnnotationConfigApplicationContext(SpringConfig.class);
        try {
            telegramBotsApi.registerBot(ctx.getBean(DecryptoBot.class));
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }

    }

}
