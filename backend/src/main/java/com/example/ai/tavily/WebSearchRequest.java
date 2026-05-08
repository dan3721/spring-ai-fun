package com.example.ai.tavily;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Tool argument shape for {@code web_search}. Ollama / Spring AI pass JSON with a {@code query} field.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record WebSearchRequest(String query) {}
