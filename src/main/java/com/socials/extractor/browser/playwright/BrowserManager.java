package com.socials.extractor.browser.playwright;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Playwright;
import com.socials.extractor.model.BrowserSession;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
public class BrowserManager {

    private Playwright playwright;

    private Browser browser;

    @PostConstruct
    public void start() {

        playwright = Playwright.create();

        browser = playwright.chromium()
                .launch(
                        new BrowserType.LaunchOptions()
                                .setHeadless(true)
                );

        log.info("================================");
        log.info("Playwright Browser Started");
        log.info("================================");

    }

    public Browser browser() {

        return browser;

    }

    /**
     * Creates a fresh BrowserContext configuration
     * for each extraction request.
     */
    public Browser.NewContextOptions createContextOptions(
            BrowserSession session
    ) {

        return new Browser.NewContextOptions()

                .setUserAgent(session.getUserAgent())

                .setLocale("en-US");

    }

    /**
     * Configure network routing and default request headers.
     */
    public void configureRouting(
            BrowserContext context,
            BrowserSession session
    ) {

        context.setExtraHTTPHeaders(

                Map.of(

                        "Accept-Language",
                        session.getAcceptLanguage(),

                        "Referer",
                        session.getReferer(),

                        "Accept-Encoding",
                        session.getAcceptEncoding()

                )

        );

        context.route("**/*", route -> {

            String resourceType =
                    route.request().resourceType();

            if ("font".equals(resourceType)) {

                route.abort();

                return;

            }

            route.resume();

        });

    }

    @PreDestroy
    public void shutdown() {

        if (browser != null) {

            browser.close();

        }

        if (playwright != null) {

            playwright.close();

        }

        log.info("================================");
        log.info("Playwright Browser Stopped");
        log.info("================================");

    }

}