package com.socials.extractor.browser.playwright;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Page;
import com.socials.extractor.browser.BrowserCapture;
import com.socials.extractor.browser.BrowserClient;
import com.socials.extractor.browser.playwright.capture.*;
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
public class PlaywrightBrowserClient implements BrowserClient {

    private final BrowserManager browserManager;
    private final RequestCollector requestCollector;
    private final ResponseCollector responseCollector;
    private final PageInteractor pageInteractor;
    private final ResponseStabilizer responseStabilizer;
    private final CaptureBuilder captureBuilder;

    @Override
    public Mono<BrowserCapture> capture(String url, BrowserSession session) {
        return Mono.fromCallable(() -> {
            Browser browser = browserManager.browser();
            BrowserContext context = browser.newContext(browserManager.createContextOptions(session));
            browserManager.configureRouting(context, session);

            try (Page page = context.newPage()) {
                CaptureState state = new CaptureState();

                requestCollector.register(page, state);
                responseCollector.register(page, state);

                pageInteractor.open(page, url);
                responseStabilizer.waitForStable(page, state);

                return captureBuilder.build(page, state);
            } finally {
                context.close();
            }

        });
    }
}