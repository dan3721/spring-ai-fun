package com.example.ai.chat;

import java.util.LinkedHashSet;
import java.util.List;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;

/**
 * Collects tool names from chat memory deltas and from the final {@link ChatResponse} (when the
 * model exposes tool calls on assistant generations).
 */
final class ChatTurnToolNames {

    private ChatTurnToolNames() {}

    static List<String> fromChatResponse(ChatResponse response) {
        if (response == null || response.getResults() == null) {
            return List.of();
        }
        LinkedHashSet<String> orderedUnique = new LinkedHashSet<>();
        for (Generation generation : response.getResults()) {
            if (generation == null || generation.getOutput() == null) {
                continue;
            }
            if (generation.getOutput() instanceof AssistantMessage assistant && assistant.hasToolCalls()) {
                for (AssistantMessage.ToolCall call : assistant.getToolCalls()) {
                    orderedUnique.add(call.name());
                }
            }
        }
        return List.copyOf(orderedUnique);
    }

    static List<String> mergeUniqueOrdered(List<String> tracked, List<String> fromResponse, List<String> fromMemory) {
        LinkedHashSet<String> merged = new LinkedHashSet<>();
        if (tracked != null) {
            merged.addAll(tracked);
        }
        if (fromResponse != null) {
            merged.addAll(fromResponse);
        }
        if (fromMemory != null) {
            merged.addAll(fromMemory);
        }
        return List.copyOf(merged);
    }

    static List<String> fromMemoryDelta(List<Message> before, List<Message> after) {
        int start = before.size();
        if (after.size() <= start) {
            return List.of();
        }
        LinkedHashSet<String> orderedUnique = new LinkedHashSet<>();
        for (int i = start; i < after.size(); i++) {
            Message message = after.get(i);
            if (message instanceof AssistantMessage assistant && assistant.hasToolCalls()) {
                for (AssistantMessage.ToolCall call : assistant.getToolCalls()) {
                    orderedUnique.add(call.name());
                }
            } else if (message instanceof ToolResponseMessage toolResponses) {
                for (ToolResponseMessage.ToolResponse response : toolResponses.getResponses()) {
                    orderedUnique.add(response.name());
                }
            }
        }
        return List.copyOf(orderedUnique);
    }
}
