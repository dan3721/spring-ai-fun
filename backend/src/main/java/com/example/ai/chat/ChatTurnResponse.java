package com.example.ai.chat;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ChatTurnResponse {
    String role;
    String content;

    /** Present for assistant (pending calls) and tool rows: function names for this message. */
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    List<String> toolNames;
}
