package com.socials.extractor.platforms.meta.pipeline.steps;

import com.socials.extractor.browser.BrowserCapture;
import com.socials.extractor.browser.BrowserClient;
import com.socials.extractor.model.MediaResult;
import com.socials.extractor.pipeline.PipelineContext;
import com.socials.extractor.pipeline.PipelineStep;
import com.socials.extractor.platforms.meta.service.MetaMediaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@RequiredArgsConstructor
public class ResolveBrowserMediaStep
        implements PipelineStep {

    private static final int MAX_ATTEMPTS = 2;

    private final BrowserClient browserClient;

    private final MetaMediaService mediaService;

    @Override
    public int order() {

        return 30;

    }

    @Override
    public Mono<PipelineContext> execute(
            PipelineContext context
    ) {

        MediaResult media = null;

        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {

            BrowserCapture capture;

            /*
             * First attempt uses the browser capture already produced
             * by CaptureBrowserStep.
             */
            if (attempt == 1) {

                capture = context.getBrowserCapture();

            }

            /*
             * Retry performs a brand-new browser capture.
             */
            else {

                log.warn(
                        "Retrying browser capture (attempt {}).",
                        attempt
                );

                capture = browserClient

                        .capture(
                                context.getUrl(),
                                context.getBrowserSession()
                        )

                        .block();

                context.setBrowserCapture(
                        capture
                );

            }

            media = mediaService.resolve(
                    capture
            );

            if (hasPlayableVideo(media)) {

                log.info(
                        "Playable video found on attempt {}.",
                        attempt
                );

                context.setResult(
                        media
                );

                return Mono.just(
                        context
                );

            }

            log.warn(
                    "Attempt {} produced no playable video.",
                    attempt
            );

        }

        /*
         * Return the last result even if no playable video
         * was found (thumbnail/title may still exist).
         */
        context.setResult(
                media
        );

        return Mono.just(
                context
        );

    }

    private boolean hasPlayableVideo(
            MediaResult media
    ) {

        return media != null
                && media.getUrl() != null
                && !media.getUrl().isBlank();

    }

}