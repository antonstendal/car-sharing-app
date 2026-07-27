package com.example.carsharing.dto.telegram;

import com.fasterxml.jackson.annotation.JsonProperty;

public record TelegramMessageRequest(
        @JsonProperty("chat_id")
        Long chatId,
        @JsonProperty("text")
        String message
) {
}
