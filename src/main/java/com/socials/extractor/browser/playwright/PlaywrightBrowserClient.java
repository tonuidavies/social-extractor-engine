package com.socials.extractor.browser.playwright;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Page;
import com.socials.extractor.browser.BrowserCapture;
import com.socials.extractor.browser.BrowserClient;
import com.socials.extractor.browser.playwright.capture.CaptureBuilder;
import com.socials.extractor.browser.playwright.capture.PageInteractor;
import com.socials.extractor.browser.playwright.capture.RequestCollector;
import com.socials.extractor.browser.playwright.capture.ResponseCollector;
import com.socials.extractor.browser.playwright.capture.ResponseStabilizer;
import com.socials.extractor.browser.playwright.model.CaptureState;
import com.socials.extractor.model.BrowserSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Slf4j
@Primary
@Component
@RequiredArgsConstructor
public class PlaywrightBrowserClient
        implements BrowserClient {

    private final BrowserManager browserManager;

    private final RequestCollector requestCollector;

    private final ResponseCollector responseCollector;

    private final PageInteractor pageInteractor;

    private final ResponseStabilizer responseStabilizer;

    private final CaptureBuilder captureBuilder;

    @Override
    public Mono<BrowserCapture> capture(
            String url,
            BrowserSession session
    ) {

        return Mono.fromCallable(() -> {

            long totalStart = System.currentTimeMillis();

            Browser browser = browserManager.browser();

            BrowserContext context = browser.newContext(
                    browserManager.createContextOptions(session)
            );

            browserManager.configureRouting(
                    context,
                    session
            );

            try {

                Page page = context.newPage();

                CaptureState state = new CaptureState();

                /*
                 * Register listeners before navigation.
                 */
                requestCollector.register(
                        page,
                        state
                );

                responseCollector.register(
                        page,
                        state
                );

                /*
                 * Navigate.
                 */
                long t = System.currentTimeMillis();

                pageInteractor.open(
                        page,
                        url
                );

                log.info(
                        "pageInteractor.open() took {} ms",
                        System.currentTimeMillis() - t
                );

                /*
                 * Wait until network activity settles.
                 */
                t = System.currentTimeMillis();

                responseStabilizer.waitForStable(
                        page,
                        state
                );

                log.info(
                        "responseStabilizer.waitForStable() took {} ms",
                        System.currentTimeMillis() - t
                );

                /*
                 * Build capture.
                 */
                t = System.currentTimeMillis();

                BrowserCapture capture =
                        captureBuilder.build(
                                page,
                                state
                        );

                log.info(
                        "captureBuilder.build() took {} ms",
                        System.currentTimeMillis() - t
                );

                log.info(
                        "Capture completed in {} ms ({} requests, {} responses)",
                        System.currentTimeMillis() - totalStart,
                        capture.getRequests().size(),
                        capture.getResponses().size()
                );

                return capture;

            }

            finally {

                long t = System.currentTimeMillis();

                context.close();

                log.info(
                        "context.close() took {} ms",
                        System.currentTimeMillis() - t
                );

            }

        });

    }

}