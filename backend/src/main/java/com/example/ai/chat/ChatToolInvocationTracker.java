package com.example.ai.chat;

import java.util.LinkedHashSet;
import java.util.List;

/**
 * Records tool names invoked during a single {@code POST /api/chat} turn. Spring AI's
 * {@link org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor} does not persist
 * intermediate tool messages, so memory deltas alone cannot detect {@code web_search}.
 */
public final class ChatToolInvocationTracker {

    private static final ThreadLocal<Boolean> ACTIVE = new ThreadLocal<>();
    private static final ThreadLocal<LinkedHashSet<String>> NAMES = new ThreadLocal<>();

    private ChatToolInvocationTracker() {}

    /** Call once at the start of a chat turn before invoking the {@link org.springframework.ai.chat.client.ChatClient}. */
    public static void beginTracking() {
        ACTIVE.set(Boolean.TRUE);
        NAMES.set(new LinkedHashSet<>());
    }

    /** Invoked from tool implementations (e.g. Tavily) when a tool actually runs. */
    public static void recordTool(String toolName) {
        if (!Boolean.TRUE.equals(ACTIVE.get()) || toolName == null || toolName.isBlank()) {
            return;
        }
        LinkedHashSet<String> set = NAMES.get();
        if (set != null) {
            set.add(toolName.trim());
        }
    }

    /**
     * Ends tracking for this thread and returns recorded names in invocation order (deduplicated).
     * Safe to call multiple times; subsequent calls return an empty list.
     */
    public static List<String> drainAndStop() {
        try {
            LinkedHashSet<String> set = NAMES.get();
            if (set == null || set.isEmpty()) {
                return List.of();
            }
            return List.copyOf(set);
        } finally {
            ACTIVE.remove();
            NAMES.remove();
        }
    }
}
