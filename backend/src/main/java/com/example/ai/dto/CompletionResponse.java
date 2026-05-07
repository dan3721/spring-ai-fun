package com.example.ai.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class CompletionResponse {
    String response;
}
