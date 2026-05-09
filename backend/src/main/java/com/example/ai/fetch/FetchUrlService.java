package com.example.ai.fetch;

import com.example.ai.chat.ChatToolInvocationTracker;
import com.example.ai.config.FetchUrlProperties;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Locale;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Demo-only HTTP fetch for {@code fetch_url} and {@code POST /api/fetch-url}. Not hardened for
 * untrusted multi-tenant use (redirect chains, DNS rebinding, etc.).
 */
@Slf4j
@Component
public class FetchUrlService {

    private final FetchUrlProperties properties;
    private final HttpClient httpClient;

    public FetchUrlService(FetchUrlProperties properties) {
        this.properties = properties;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(Math.max(1, properties.getConnectTimeoutMs())))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    /**
     * @param recordAsToolInvocation when true, records {@code fetch_url} for {@link ChatToolInvocationTracker}
     *     (chat tool path only).
     */
    public String fetch(String rawUrl, boolean recordAsToolInvocation) {
        if (!StringUtils.hasText(rawUrl)) {
            return "No URL was provided.";
        }
        String trimmed = rawUrl.trim();
        URI uri;
        try {
            uri = new URI(trimmed);
        } catch (URISyntaxException e) {
            return "Invalid URL: " + e.getMessage();
        }

        try {
            validateTarget(uri);
        } catch (IllegalArgumentException e) {
            return e.getMessage();
        }

        HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(Duration.ofMillis(Math.max(1, properties.getReadTimeoutMs())))
                .header("User-Agent", "springboot-ai-fun/0.1 (demo)")
                .GET()
                .build();

        try {
            HttpResponse<InputStream> response =
                    httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
            int status = response.statusCode();
            if (status < 200 || status >= 300) {
                log.warn("fetch_url HTTP {} for {}", status, uri);
                return "Fetch failed: HTTP " + status;
            }

            URI finalUri = response.uri();
            try {
                validateTarget(finalUri);
            } catch (IllegalArgumentException e) {
                log.warn("fetch_url redirect target rejected {}: {}", finalUri, e.getMessage());
                return e.getMessage();
            }

            String contentType = response.headers().firstValue("Content-Type").orElse("");
            byte[] raw;
            try (InputStream in = response.body()) {
                raw = readUpTo(in, Math.max(1, properties.getMaxDownloadBytes()));
            }

            String output = bodyToText(raw, contentType, uri.toString());
            logResponseBody(finalUri, output, recordAsToolInvocation);
            if (recordAsToolInvocation) {
                ChatToolInvocationTracker.recordTool("fetch_url");
            }
            return output;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("fetch_url interrupted for {}", uri);
            return "Fetch failed: interrupted";
        } catch (IOException e) {
            log.warn("fetch_url failed for {}: {}", uri, e.getMessage());
            return "Fetch failed: " + e.getMessage();
        }
    }

    private void logResponseBody(URI finalUri, String output, boolean toolPath) {
        int cap = properties.getLogResponseMaxChars();
        String mode = toolPath ? "tool" : "exercise";
        if (output == null) {
            log.info("fetch_url {} finalUri={} outputChars=0 (null)", mode, finalUri);
            return;
        }
        if (cap <= 0) {
            log.info("fetch_url {} finalUri={} outputChars={}", mode, finalUri, output.length());
            return;
        }
        int n = Math.min(cap, output.length());
        String chunk = output.substring(0, n);
        if (output.length() > cap) {
            log.info(
                    "fetch_url {} finalUri={} outputChars={} (log preview first {} chars):\n{}",
                    mode,
                    finalUri,
                    output.length(),
                    cap,
                    chunk);
            log.info("fetch_url {} ... ({} more chars omitted from log)", mode, output.length() - cap);
        } else {
            log.info("fetch_url {} finalUri={} outputChars={}:\n{}", mode, finalUri, output.length(), chunk);
        }
    }

    private void validateTarget(URI uri) {
        String scheme = uri.getScheme();
        if (scheme == null) {
            throw new IllegalArgumentException("URL must include a scheme (https://...).");
        }
        boolean https = "https".equalsIgnoreCase(scheme);
        boolean httpAllowed = properties.isAllowHttp() && "http".equalsIgnoreCase(scheme);
        if (!https && !httpAllowed) {
            throw new IllegalArgumentException(
                    "Only https URLs are allowed" + (properties.isAllowHttp() ? " (or http when allow-http is true)." : "."));
        }

        String host = uri.getHost();
        if (!StringUtils.hasText(host)) {
            throw new IllegalArgumentException("URL has no host.");
        }

        String h = host.toLowerCase(Locale.ROOT);
        if ("localhost".equals(h)
                || "127.0.0.1".equals(h)
                || "::1".equals(h)
                || "0.0.0.0".equals(h)) {
            throw new IllegalArgumentException("That host is not allowed for demo fetch_url.");
        }

        try {
            InetAddress addr = InetAddress.getByName(host);
            if (addr.isLoopbackAddress()
                    || addr.isLinkLocalAddress()
                    || addr.isSiteLocalAddress()
                    || addr.isMulticastAddress()) {
                throw new IllegalArgumentException("That address is not allowed for demo fetch_url.");
            }
        } catch (UnknownHostException e) {
            throw new IllegalArgumentException("Unknown host: " + host);
        }
    }

    private static byte[] readUpTo(InputStream in, int maxBytes) throws IOException {
        try (InputStream stream = in) {
            byte[] buf = new byte[8192];
            ByteArrayOutputStream out = new ByteArrayOutputStream(Math.min(maxBytes, 8192));
            int total = 0;
            while (total < maxBytes) {
                int toRead = Math.min(buf.length, maxBytes - total);
                int n = stream.read(buf, 0, toRead);
                if (n < 0) {
                    break;
                }
                out.write(buf, 0, n);
                total += n;
            }
            return out.toByteArray();
        }
    }

    private String bodyToText(byte[] raw, String contentTypeHeader, String baseUri) {
        Charset charset = StandardCharsets.UTF_8;
        if (StringUtils.hasText(contentTypeHeader)) {
            try {
                MediaType mt = MediaType.parseMediaType(contentTypeHeader);
                if (mt.getCharset() != null) {
                    charset = mt.getCharset();
                }
            } catch (Exception ignored) {
                // keep UTF-8
            }
        }

        String asString = new String(raw, charset);
        String lowerCt = contentTypeHeader == null ? "" : contentTypeHeader.toLowerCase(Locale.ROOT);

        if (lowerCt.contains("text/html") || lowerCt.contains("application/xhtml+xml")) {
            var doc = Jsoup.parse(asString, baseUri);
            doc.select("script, style, noscript").remove();
            String text = doc.body() != null ? doc.body().text() : doc.text();
            return truncate(normalizeWs(text), properties.getMaxResponseChars());
        }

        return truncate(normalizeWs(asString), properties.getMaxResponseChars());
    }

    private static String normalizeWs(String s) {
        if (s == null || s.isEmpty()) {
            return "";
        }
        return s.replaceAll("\\s+", " ").trim();
    }

    private static String truncate(String s, int maxChars) {
        if (s == null) {
            return "";
        }
        if (maxChars <= 0) {
            return "";
        }
        if (s.length() <= maxChars) {
            return s;
        }
        return s.substring(0, maxChars) + "\n…(truncated)";
    }
}
