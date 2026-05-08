package com.example.ai.tavily;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "tavily")
public class TavilyProperties {

    /** Tavily Search API: max results (0–20). */
    private int maxResults = 5;

    /** Tavily search_depth: basic, advanced, fast, ultra-fast (credits differ). */
    private String searchDepth = "basic";
}
