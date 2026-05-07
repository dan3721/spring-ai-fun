package com.example.ai.controller;

import com.example.ai.dto.ChatHistoryResponse;
import com.example.ai.dto.ChatMessageRequest;
import com.example.ai.dto.ChatMessageResponse;
import com.example.ai.dto.ChatTurnResponse;
import com.google.common.base.Stopwatch;
import jakarta.validation.Valid;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final ChatClient conversationChatClient;
    private final ChatMemory chatMemory;

    public ChatController(
            @Qualifier("conversationChatClient") ChatClient conversationChatClient,
            ChatMemory chatMemory) {
        this.conversationChatClient = conversationChatClient;
        this.chatMemory = chatMemory;
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ChatMessageResponse chat(@Valid @RequestBody ChatMessageRequest request) {
        String conversationId = resolveConversationId(request.getConversationId());
        Stopwatch stopwatch = Stopwatch.createStarted();
        try {
            String content = conversationChatClient
                    .prompt()
                    .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationId))
                    .user(request.getMessage())
                    .call()
                    .content();

            return ChatMessageResponse.builder()
                    .conversationId(conversationId)
                    .content(content == null ? "" : content)
                    .build();
        } finally {
            log.info(
                    "chat message duration {} (conversationId={}, messageChars={})",
                    stopwatch.stop(),
                    conversationId,
                    request.getMessage().length());
        }
    }

    @GetMapping(path = "/{conversationId}/messages", produces = MediaType.APPLICATION_JSON_VALUE)
    public ChatHistoryResponse history(@PathVariable String conversationId) {
        List<Message> stored = chatMemory.get(conversationId);
        List<ChatTurnResponse> turns = new ArrayList<>(stored.size());
        for (Message message : stored) {
            turns.add(ChatTurnResponse.builder()
                    .role(mapRole(message.getMessageType()))
                    .content(message.getText() == null ? "" : message.getText())
                    .build());
        }
        return ChatHistoryResponse.builder()
                .conversationId(conversationId)
                .messages(turns)
                .build();
    }

    private static String resolveConversationId(String raw) {
        if (raw == null || raw.isBlank()) {
            return UUID.randomUUID().toString();
        }
        return raw.trim();
    }

    private static String mapRole(MessageType type) {
        if (type == MessageType.USER) {
            return "user";
        }
        if (type == MessageType.ASSISTANT) {
            return "assistant";
        }
        if (type == MessageType.SYSTEM) {
            return "system";
        }
        if (type == MessageType.TOOL) {
            return "tool";
        }
        return type != null ? type.getValue() : "unknown";
    }
}
