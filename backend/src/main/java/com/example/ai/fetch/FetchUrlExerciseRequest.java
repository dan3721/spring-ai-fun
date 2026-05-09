package com.example.ai.fetch;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class FetchUrlExerciseRequest {

    @NotBlank(message = "url is required")
    private String url;
}
