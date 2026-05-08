package com.example.ai.search;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SearchExerciseRequest {

    @NotBlank(message = "query is required")
    private String query;
}
