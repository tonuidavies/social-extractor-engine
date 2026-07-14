package com.socials.extractor.platforms.meta.pipeline.steps;

import com.socials.extractor.browser.BrowserSessionManager;
import com.socials.extractor.pipeline.PipelineContext;
import com.socials.extractor.pipeline.PipelineStep;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class CreateBrowserSessionStep
        implements PipelineStep {

    private final BrowserSessionManager sessionManager;

    @Override
    public int order() {
        return 10;
    }

    @Override
    public Mono<PipelineContext> execute(
            PipelineContext context
    ) {

        context.setBrowserSession(

                sessionManager.create()

        );

        return Mono.just(context);

    }

}