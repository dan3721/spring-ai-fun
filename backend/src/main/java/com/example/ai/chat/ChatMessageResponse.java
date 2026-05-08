package com.example.ai.chat;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ChatMessageResponse {
    String conversationId;
    String content;

    /** Tool functions invoked during this turn (e.g. {@code web_search}), in call order. */
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    List<String> toolsUsed;
}
