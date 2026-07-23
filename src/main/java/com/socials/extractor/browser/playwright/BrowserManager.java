package com.socials.extractor.browser.playwright;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.options.Proxy;
import com.socials.extractor.model.BrowserSession;
import com.socials.extractor.network.ProxySettings;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class BrowserManager {

    private final ProxySettings proxySettings;

    private Playwright playwright;
    private Browser browser;

    private static final Set<String> BLOCKED_RESOURCES =
            Set.of("image", "stylesheet", "font");

    @PostConstruct
    public void start() {
        playwright = Playwright.create();

        BrowserType.LaunchOptions options =
                new BrowserType.LaunchOptions().setHeadless(true);

        if (proxySettings.isEnabled()) {
            Proxy proxy = new Proxy("http://" + proxySettings.getHost() + ":" + proxySettings.getPort());
            if (proxySettings.getUsername() != null && !proxySettings.getUsername().isBlank()) {
                proxy.setUsername(proxySettings.getUsername())
                        .setPassword(proxySettings.getPassword());
            }
            options.setProxy(proxy);
            log.info("Playwright launching behind proxy {}:{}", proxySettings.getHost(), proxySettings.getPort());
        }

        browser = playwright.chromium().launch(options);

        log.info("================================");
        log.info("Playwright Browser Started");
        log.info("================================");
    }

    public Browser browser() {
        return browser;
    }

    public Browser.NewContextOptions createContextOptions(BrowserSession session) {
        return new Browser.NewContextOptions()
                .setUserAgent(session.getUserAgent())
                .setLocale("en-US");
    }

    public void configureRouting(BrowserContext context, BrowserSession session) {

        Map<String, String> headers = new HashMap<>();
        if (session.getAcceptLanguage() != null) headers.put("Accept-Language", session.getAcceptLanguage());
        if (session.getReferer() != null) headers.put("Referer", session.getReferer());
        if (session.getAcceptEncoding() != null) headers.put("Accept-Encoding", session.getAcceptEncoding());
        if (!headers.isEmpty()) context.setExtraHTTPHeaders(headers);

        context.route("**/*", route -> {
            String resourceType = route.request().resourceType();
            if (BLOCKED_RESOURCES.contains(resourceType)) {
                route.abort();
            } else {
                route.resume();
            }
        });
    }

    @PreDestroy
    public void shutdown() {
        if (browser != null) browser.close();
        if (playwright != null) playwright.close();

        log.info("================================");
        log.info("Playwright Browser Stopped");
        log.info("================================");
    }
}