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
        return BrowserCapture.builder()
                .finalUrl(safeUrl(page))
                .html(safeContent(page))
                .requests(state.getRequests())
                .responses(state.getResponses())
                .build();
    }

    private String safeContent(Page page) {
        for (int attempt = 0; attempt < 3; attempt++) {
            try {
                return page.content();
            } catch (Exception e) {
                try {
                    page.waitForTimeout(600);
                } catch (Exception ignore) {
                }
            }
        }
        // Last resort: read the rendered DOM directly.
        try {
            Object html = page.evaluate("document.documentElement.outerHTML");
            if (html != null) return String.valueOf(html);
        } catch (Exception e) {
            log.warn("Failed to capture page content: {}", e.getMessage());
        }
        return "";
    }

    private String safeUrl(Page page) {
        try {
            return page.url();
        } catch (Exception e) {
            return "";
        }
    }
}