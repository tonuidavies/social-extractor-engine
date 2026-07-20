package com.socials.extractor.platforms.tiktok.pipeline.steps;

import com.socials.extractor.browser.BrowserSessionManager;
import com.socials.extractor.pipeline.PipelineContext;
import com.socials.extractor.pipeline.PipelineStep;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class CreateTikTokSessionStep implements PipelineStep {

    private final BrowserSessionManager sessionManager;

    @Override
    public int order() {
        return 10; // Must run before CaptureTikTokBrowserStep (20)
    }

    @Override
    public Mono<PipelineContext> execute(PipelineContext context) {
        return Mono.fromCallable(() -> sessionManager.create()) // Uses the correct 'create()' method[cite: 1]
                .map(session -> {
                    context.setBrowserSession(session);
                    return context;
                });
    }
}