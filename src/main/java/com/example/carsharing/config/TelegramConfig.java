package com.example.carsharing.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Getter
@Component
public class TelegramConfig {
    @Value("${telegram.bot-token}")
    private String token;
    @Value("${telegram.chat-id}")
    private Long chatId;
}
