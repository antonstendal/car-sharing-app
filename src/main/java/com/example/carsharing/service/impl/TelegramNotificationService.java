package com.example.carsharing.service.impl;

import com.example.carsharing.config.TelegramConfig;
import com.example.carsharing.dto.telegram.TelegramMessageRequest;
import com.example.carsharing.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@RequiredArgsConstructor
@Service
public class TelegramNotificationService implements NotificationService {
    public static final String TELEGRAM_ACTION_SEND_MESSAGE = "/sendMessage";
    public static final String TELEGRAM_DOMAIN_BOT = "https://api.telegram.org/bot";
    private final TelegramConfig telegramConfig;
    private final RestClient restClient;

    @Override
    public void send(String message) {
        String url = TELEGRAM_DOMAIN_BOT
                + telegramConfig.getToken()
                + TELEGRAM_ACTION_SEND_MESSAGE;
        TelegramMessageRequest request = new TelegramMessageRequest(
                telegramConfig.getChatId(), message);
        restClient
                .post()
                .uri(url)
                .body(request)
                .retrieve()
                .toBodilessEntity();
    }
}
