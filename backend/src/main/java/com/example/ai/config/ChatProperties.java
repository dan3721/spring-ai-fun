package com.example.ai.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "app.chat")
public class ChatProperties {

    /**
     * System prompt for multi-turn chat ({@code conversationChatClient}). Set in
     * {@code application.yml} as {@code app.chat.system-instruction}. When empty, a built-in
     * default in {@link AiChatClientConfig} is used.
     */
    private String systemInstruction = "";
}
