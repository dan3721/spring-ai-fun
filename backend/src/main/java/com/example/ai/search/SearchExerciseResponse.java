package com.example.ai.search;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class SearchExerciseResponse {
    String query;
    String output;
}
