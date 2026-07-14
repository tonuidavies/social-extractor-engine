package com.socials.extractor.platforms.meta.pipeline.steps;

import com.socials.extractor.browser.BrowserClient;
import com.socials.extractor.pipeline.PipelineContext;
import com.socials.extractor.pipeline.PipelineStep;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class CaptureBrowserStep
        implements PipelineStep {

    private final BrowserClient browserClient;

    @Override
    public int order() {
        return 20;
    }
    @Override
    public Mono<PipelineContext> execute(
            PipelineContext context
    ) {

        System.out.println();
        System.out.println("================================");
        System.out.println("BrowserClient Bean:");
        System.out.println(browserClient.getClass().getName());
        System.out.println("================================");

        return browserClient

                .capture(
                        context.getUrl(),
                        context.getBrowserSession()
                )

                .map(capture -> {

                    System.out.println(
                            "Captured responses = "
                                    + capture.getResponses().size()
                    );

                    context.setBrowserCapture(capture);

                    return context;

                });

    }

}