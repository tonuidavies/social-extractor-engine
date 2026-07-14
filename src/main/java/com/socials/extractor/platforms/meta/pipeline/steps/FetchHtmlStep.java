package com.socials.extractor.platforms.meta.pipeline.steps;

import com.socials.extractor.browser.BrowserClient;
import com.socials.extractor.pipeline.PipelineContext;
import com.socials.extractor.pipeline.PipelineStep;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

//@Component
@RequiredArgsConstructor
public class FetchHtmlStep
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

        return browserClient

                .capture(
                        context.getUrl(),
                        context.getBrowserSession()
                )

                .map(capture -> {

                    context.setHtml(
                            capture.getHtml()
                    );

                    try {

                        Files.writeString(

                                Path.of("/tmp/meta.html"),

                                capture.getHtml()

                        );

                    }

                    catch (IOException ignored) {
                    }

                    return context;

                });

    }

}