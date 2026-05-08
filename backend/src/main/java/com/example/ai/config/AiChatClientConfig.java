package com.example.ai.config;

import com.example.ai.tavily.TavilySearchClient;
import com.example.ai.tavily.WebSearchRequest;
import java.util.function.Function;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.function.FunctionToolCallback;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

@Slf4j
@Configuration
public class AiChatClientConfig {

    private static final String DEFAULT_CHAT_SYSTEM =
            """
            You are a helpful assistant.

            Answer from your own knowledge and this conversation. Do not call web_search unless this turn \
            includes that tool and the user has explicitly asked for a web or internet search.

            When web_search is available, use it only after the user clearly requests searching the web, \
            searching online, a web search, or similar — never for routine questions you can answer yourself.
            """
                    .strip();

    private static final String WEB_SEARCH_TOOL_DESCRIPTION =
            "Public web search via Tavily. Use ONLY in this turn because the user explicitly asked to search "
                    + "the web / internet / online (e.g. they said 'web search', 'search the web', "
                    + "'search online'). Do not use for normal chat, coding help, or facts you already know. "
                    + "Argument: one short search query string.";

    @Bean(name = "completionChatClient")
    public ChatClient completionChatClient(OllamaChatModel ollamaChatModel) {
        return ChatClient.builder(ollamaChatModel).build();
    }

    @Bean(name = "conversationChatClient")
    public ChatClient conversationChatClient(
            OllamaChatModel ollamaChatModel,
            ChatMemory chatMemory,
            ChatProperties chatProperties,
            @Value("${TAVILY_API_KEY:}") String tavilyApiKey) {

        String systemPrompt = StringUtils.hasText(chatProperties.getSystemInstruction())
                ? chatProperties.getSystemInstruction().trim()
                : DEFAULT_CHAT_SYSTEM;

        if (!StringUtils.hasText(tavilyApiKey)) {
            log.warn("TAVILY_API_KEY not set — web_search unavailable for /api/chat");
        }

        return ChatClient.builder(ollamaChatModel)
                .defaultSystem(systemPrompt)
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
                .build();
    }

    @Bean(name = "webSearchToolCallback")
    @Conditional(TavilyApiKeyPresentCondition.class)
    public ToolCallback webSearchToolCallback(TavilySearchClient tavilySearchClient) {
        Function<WebSearchRequest, String> runWebSearch = req -> tavilySearchClient.search(req.query());
        ToolCallback callback = FunctionToolCallback.builder("web_search", runWebSearch)
                .description(WEB_SEARCH_TOOL_DESCRIPTION)
                .inputType(WebSearchRequest.class)
                .build();
        log.info(
                "web_search tool registered (offered on /api/chat only when the user explicitly requests a web search)");
        return callback;
    }
}
