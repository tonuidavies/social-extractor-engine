package com.socials.extractor.browser.playwright.capture;

import com.microsoft.playwright.Page;
import com.socials.extractor.browser.BrowserResponse;
import com.socials.extractor.browser.playwright.model.CaptureState;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ResponseCollector {

    public void register(Page page, CaptureState state) {
        page.onResponse(response -> {
            try {
                String resourceType = response.request().resourceType();
                String contentType = response.headers().getOrDefault("content-type", "");
                String url = response.url();

                // Log only crucial events
                if (log.isDebugEnabled()) {
                    log.debug("Network Response: [{}] {}", resourceType, url);
                }

                byte[] body = null;
                // Only capture text/json payloads
                if (contentType.contains("json")
                        || contentType.contains("javascript")
                        || contentType.startsWith("text/")) {
                    try {
                        body = response.body();
                    } catch (Exception ex) {
                        log.debug("Could not read response body for: {}", url);
                    }
                }

                state.getResponses().add(BrowserResponse.builder()
                        .url(url)
                        .method(response.request().method())
                        .resourceType(resourceType)
                        .status(response.status())
                        .contentType(contentType)
                        .headers(response.headers())
                        .body(body)
                        .build());
            } catch (Exception ex) {
                log.debug("Error capturing response: {}", ex.getMessage());
            }
        });
    }
}