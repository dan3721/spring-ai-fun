package com.example.ai.completion;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CompletionRequest {

    @NotBlank(message = "prompt is required")
    private String prompt;
}
