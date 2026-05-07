package com.example.ai.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ChatMessageRequest {

    /** When null or blank, the server assigns a new id for a fresh conversation. */
    private String conversationId;

    @NotBlank(message = "message is required")
    private String message;
}
