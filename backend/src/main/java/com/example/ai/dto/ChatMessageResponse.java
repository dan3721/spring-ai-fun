package com.example.ai.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ChatMessageResponse {
    String conversationId;
    String content;
}
