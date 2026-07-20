package com.socials.extractor.platforms.tiktok.pipeline.steps;

import com.socials.extractor.browser.BrowserClient;
import com.socials.extractor.pipeline.PipelineContext;
import com.socials.extractor.pipeline.PipelineStep;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class CaptureTikTokBrowserStep implements PipelineStep {
    private final BrowserClient browserClient;

    @Override
    public int order() { return 20; }

    @Override
    public Mono<PipelineContext> execute(PipelineContext context) {
        return browserClient.capture(context.getUrl(), context.getBrowserSession())
                .map(capture -> {
                    context.setBrowserCapture(capture);
                    return context;
                });
    }
}