package com.example.ai.fetch;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class FetchUrlExerciseResponse {
    String url;
    String output;
}
