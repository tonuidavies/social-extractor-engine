package com.socials.extractor.browser.playwright.capture;

import com.microsoft.playwright.Page;
import com.socials.extractor.browser.BrowserCapture;
import com.socials.extractor.browser.playwright.model.CaptureState;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class CaptureBuilder {

    public BrowserCapture build(Page page, CaptureState state) {
        String html = "";
        try {
            html = page.content();
        } catch (Exception ex) {
            log.warn("Failed to capture page content: {}", ex.getMessage());
        }

        return BrowserCapture.builder()
                .finalUrl(page.url())
                .html(html)
                .requests(state.getRequests())
                .responses(state.getResponses())
                .build();
    }
}