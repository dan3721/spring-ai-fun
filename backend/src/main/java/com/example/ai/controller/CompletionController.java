package com.example.ai.controller;

import com.example.ai.dto.CompletionRequest;
import com.example.ai.dto.CompletionResponse;
import com.google.common.base.Stopwatch;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Single-shot text completion (one prompt in, one completion out). For multi-turn
 * conversational APIs, use a separate {@code /api/chat} style controller later.
 */
@Slf4j
@RestController
@RequestMapping("/api/completion")
public class CompletionController {

    private final ChatClient chatClient;

    public CompletionController(@Qualifier("completionChatClient") ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public CompletionResponse complete(@Valid @RequestBody CompletionRequest request) {
        Stopwatch stopwatch = Stopwatch.createStarted();
        try {
            String content = chatClient
                    .prompt()
                    .user(request.getPrompt())
                    .call()
                    .content();

            return CompletionResponse.builder()
                    .response(content == null ? "" : content)
                    .build();
        } finally {
            log.info(
                    "completion request duration {} (promptChars={})",
                    stopwatch.stop(),
                    request.getPrompt().length());
        }
    }
}
