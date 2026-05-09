package com.example.ai.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "app.fetch-url")
public class FetchUrlProperties {

    /** Max characters returned to the model or exercise API after extraction. */
    private int maxResponseChars = 12_000;

    /** Max raw bytes read from the HTTP response body. */
    private int maxDownloadBytes = 2_000_000;

    private int connectTimeoutMs = 5_000;

    private int readTimeoutMs = 15_000;

    /** When false, only https URLs are allowed (recommended). */
    private boolean allowHttp = false;

    /**
     * At INFO, log this many characters of the returned fetch text (tool + exercise). {@code 0} logs only
     * the character count, not the body.
     */
    private int logResponseMaxChars = 8192;
}
