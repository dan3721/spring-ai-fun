package com.example.ai.chat;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Gates whether the {@code web_search} tool is offered for this turn. Requires the user to ask for a
 * web/online search in natural language so the model cannot auto-invoke Tavily for weather/news, etc.
 */
public final class WebSearchGating {

    /** Match if any pattern hits the user message (case-insensitive). */
    private static final List<Pattern> EXPLICIT_REQUEST_PATTERNS = List.of(
            Pattern.compile("\\bweb\\s*search\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\bsearch\\s+(the\\s+)?web\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\bsearch\\s+online\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\bsearch\\s+the\\s+internet\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\blook\\s+(that\\s+)?up\\s+online\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\blook\\s+it\\s+up\\s+on\\s+the\\s+web\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\bcan\\s+you\\s+search\\s+(the\\s+)?(web|internet)\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\bplease\\s+(do\\s+a\\s+)?(web\\s*search|internet\\s+search)\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\bgo\\s+search\\s+(the\\s+)?(web|internet)\\s+for\\b", Pattern.CASE_INSENSITIVE));

    private WebSearchGating() {}

    public static boolean userRequestedWebSearch(String message) {
        if (message == null || message.isBlank()) {
            return false;
        }
        String text = message.trim();
        return EXPLICIT_REQUEST_PATTERNS.stream().anyMatch(p -> p.matcher(text).find());
    }
}
