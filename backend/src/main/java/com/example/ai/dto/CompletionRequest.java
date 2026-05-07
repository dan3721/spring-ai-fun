package com.example.ai.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CompletionRequest {

    @NotBlank(message = "prompt is required")
    private String prompt;
}
