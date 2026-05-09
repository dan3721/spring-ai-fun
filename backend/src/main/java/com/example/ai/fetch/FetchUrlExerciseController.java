package com.example.ai.fetch;

import com.google.common.base.Stopwatch;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Direct URL fetch exercise (no LLM). For debugging {@code fetch_url} and comparing with chat tool output.
 */
@Slf4j
@RestController
@RequestMapping("/api/fetch-url")
public class FetchUrlExerciseController {

    private final FetchUrlService fetchUrlService;

    public FetchUrlExerciseController(FetchUrlService fetchUrlService) {
        this.fetchUrlService = fetchUrlService;
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public FetchUrlExerciseResponse fetch(@Valid @RequestBody FetchUrlExerciseRequest request) {
        String url = request.getUrl().trim();
        Stopwatch stopwatch = Stopwatch.createStarted();
        String output = fetchUrlService.fetch(url, false);
        log.info(
                "fetch-url exercise completed in {} (urlChars={})",
                stopwatch.stop(),
                StringUtils.hasText(url) ? url.length() : 0);
        return FetchUrlExerciseResponse.builder().url(url).output(output).build();
    }
}
