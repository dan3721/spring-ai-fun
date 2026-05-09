package com.example.ai.fetch;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Tool argument shape for {@code fetch_url}. Spring AI / Ollama pass JSON with a {@code url} field.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record FetchUrlRequest(String url) {}
