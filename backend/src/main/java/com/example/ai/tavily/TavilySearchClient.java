package com.example.ai.tavily;

import com.example.ai.chat.ChatToolInvocationTracker;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Slf4j
@Component
public class TavilySearchClient {

    private static final int MAX_RESPONSE_CHARS = 12_000;

    private final String apiKey;
    private final TavilyProperties properties;
    private final ObjectMapper objectMapper;
    private final RestClient restClient;

    public TavilySearchClient(
            @Value("${TAVILY_API_KEY:}") String apiKey,
            TavilyProperties properties,
            ObjectMapper objectMapper) {
        this.apiKey = apiKey;
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.restClient = RestClient.builder().baseUrl("https://api.tavily.com").build();
    }

    /**
     * Runs a Tavily search and returns a compact text block for the model. Requires {@code TAVILY_API_KEY}.
     */
    public String search(String query) {
        if (!StringUtils.hasText(apiKey)) {
            throw new IllegalStateException("TAVILY_API_KEY is not set");
        }
        if (!StringUtils.hasText(query)) {
            return "No search query was provided.";
        }

        Map<String, Object> body = new HashMap<>();
        body.put("query", query.trim());
        body.put("max_results", Math.clamp(properties.getMaxResults(), 0, 20));
        body.put("search_depth", properties.getSearchDepth());

        long t0 = System.nanoTime();
        try {
            String raw = restClient
                    .post()
                    .uri("/search")
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + apiKey)
                    .body(body)
                    .retrieve()
                    .body(String.class);

            String formatted = formatResults(raw, query);
            ChatToolInvocationTracker.recordTool("web_search");
            log.info(
                    "Tavily search completed in {} ms (queryChars={})",
                    (System.nanoTime() - t0) / 1_000_000,
                    query.length());
            return formatted;
        } catch (RestClientException ex) {
            log.warn("Tavily request failed: {}", ex.getMessage());
            return "Web search failed: " + ex.getMessage();
        } catch (Exception ex) {
            log.warn("Tavily response handling failed", ex);
            return "Web search failed: " + ex.getMessage();
        }
    }

    private String formatResults(String json, String originalQuery) throws Exception {
        JsonNode root = objectMapper.readTree(json);
        StringBuilder sb = new StringBuilder();
        sb.append("Search results for: ").append(originalQuery.trim()).append("\n\n");

        JsonNode results = root.get("results");
        if (results == null || !results.isArray() || results.isEmpty()) {
            sb.append("(No results returned.)");
            return truncate(sb.toString());
        }

        int i = 1;
        for (JsonNode hit : results) {
            String title = textOrEmpty(hit.get("title"));
            String url = textOrEmpty(hit.get("url"));
            String content = textOrEmpty(hit.get("content"));
            sb.append(i++)
                    .append(". ")
                    .append(title)
                    .append("\n   URL: ")
                    .append(url)
                    .append("\n   ")
                    .append(content.replace("\n", " ").trim())
                    .append("\n\n");
            if (sb.length() >= MAX_RESPONSE_CHARS) {
                break;
            }
        }
        return truncate(sb.toString());
    }

    private static String textOrEmpty(JsonNode n) {
        if (n == null || n.isNull()) {
            return "";
        }
        return n.asText("").trim();
    }

    private static String truncate(String s) {
        if (s.length() <= MAX_RESPONSE_CHARS) {
            return s;
        }
        return s.substring(0, MAX_RESPONSE_CHARS) + "\n…(truncated)";
    }
}
