package com.example.ai.search;

import com.example.ai.tavily.TavilySearchClient;
import com.google.common.base.Stopwatch;
import jakarta.validation.Valid;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Direct Tavily exercise endpoint (no LLM). For debugging and verifying API keys.
 */
@Slf4j
@RestController
@RequestMapping("/api/search")
public class SearchController {

    private final TavilySearchClient tavilySearchClient;
    private final String tavilyApiKey;

    public SearchController(
            TavilySearchClient tavilySearchClient, @Value("${TAVILY_API_KEY:}") String tavilyApiKey) {
        this.tavilySearchClient = tavilySearchClient;
        this.tavilyApiKey = tavilyApiKey;
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> search(@Valid @RequestBody SearchExerciseRequest request) {
        if (!StringUtils.hasText(tavilyApiKey)) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of(
                            "error", "TAVILY_API_KEY is not set",
                            "hint", "Add the key to a repo-root .env file or export it before starting the backend."));
        }

        Stopwatch stopwatch = Stopwatch.createStarted();
        try {
            String output = tavilySearchClient.search(request.getQuery());
            log.info("search exercise completed in {} (queryChars={})", stopwatch.stop(), request.getQuery().length());
            return ResponseEntity.ok(SearchExerciseResponse.builder()
                    .query(request.getQuery().trim())
                    .output(output)
                    .build());
        } catch (IllegalStateException ex) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("error", ex.getMessage()));
        }
    }
}
