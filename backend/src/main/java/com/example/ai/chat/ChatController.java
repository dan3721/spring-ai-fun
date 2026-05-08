package com.example.ai.chat;

import com.google.common.base.Stopwatch;
import jakarta.validation.Valid;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.annotation.Autowired;
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
    private final ToolCallback webSearchToolCallback;

    public ChatController(
            @Qualifier("conversationChatClient") ChatClient conversationChatClient,
            ChatMemory chatMemory,
            @Autowired(required = false) @Qualifier("webSearchToolCallback") ToolCallback webSearchToolCallback) {
        this.conversationChatClient = conversationChatClient;
        this.chatMemory = chatMemory;
        this.webSearchToolCallback = webSearchToolCallback;
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ChatMessageResponse chat(@Valid @RequestBody ChatMessageRequest request) {
        String conversationId = resolveConversationId(request.getConversationId());
        List<Message> memoryBefore = List.copyOf(chatMemory.get(conversationId));
        ChatToolInvocationTracker.beginTracking();
        Stopwatch stopwatch = Stopwatch.createStarted();
        try {
            var promptSpec = conversationChatClient
                    .prompt()
                    .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationId))
                    .user(request.getMessage());

            if (webSearchToolCallback != null
                    && WebSearchGating.userRequestedWebSearch(request.getMessage())) {
                promptSpec = promptSpec.toolCallbacks(webSearchToolCallback);
            }

            ChatResponse chatResponse = promptSpec.call().chatResponse();

            String content = contentFrom(chatResponse);
            List<String> toolsUsed = ChatTurnToolNames.mergeUniqueOrdered(
                    ChatToolInvocationTracker.drainAndStop(),
                    ChatTurnToolNames.fromChatResponse(chatResponse),
                    ChatTurnToolNames.fromMemoryDelta(memoryBefore, chatMemory.get(conversationId)));

            return ChatMessageResponse.builder()
                    .conversationId(conversationId)
                    .content(content)
                    .toolsUsed(toolsUsed)
                    .build();
        } finally {
            ChatToolInvocationTracker.drainAndStop();
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
            turns.add(toTurn(message));
        }
        return ChatHistoryResponse.builder()
                .conversationId(conversationId)
                .messages(turns)
                .build();
    }

    private static ChatTurnResponse toTurn(Message message) {
        ChatTurnResponse.ChatTurnResponseBuilder builder = ChatTurnResponse.builder()
                .role(mapRole(message.getMessageType()))
                .content(message.getText() == null ? "" : message.getText());

        if (message instanceof AssistantMessage assistant && assistant.hasToolCalls()) {
            builder.toolNames(assistant.getToolCalls().stream()
                    .map(AssistantMessage.ToolCall::name)
                    .toList());
        } else if (message instanceof ToolResponseMessage toolResponses) {
            builder.toolNames(toolResponses.getResponses().stream()
                    .map(ToolResponseMessage.ToolResponse::name)
                    .toList());
        }

        return builder.build();
    }

    private static String contentFrom(ChatResponse chatResponse) {
        if (chatResponse == null) {
            return "";
        }
        Generation generation = chatResponse.getResult();
        if (generation == null || generation.getOutput() == null) {
            return "";
        }
        String text = generation.getOutput().getText();
        return text == null ? "" : text;
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
