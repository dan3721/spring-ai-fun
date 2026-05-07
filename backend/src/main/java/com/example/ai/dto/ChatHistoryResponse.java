package com.example.ai.dto;

import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ChatHistoryResponse {
    String conversationId;
    List<ChatTurnResponse> messages;
}
