package com.example.ai.config;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.util.StringUtils;

/** Registers {@code webSearchToolCallback} only when {@code TAVILY_API_KEY} is non-blank. */
public final class TavilyApiKeyPresentCondition implements Condition {

    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        return StringUtils.hasText(context.getEnvironment().getProperty("TAVILY_API_KEY", ""));
    }
}
