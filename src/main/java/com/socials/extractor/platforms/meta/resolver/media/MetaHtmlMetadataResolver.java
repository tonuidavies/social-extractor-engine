package com.socials.extractor.platforms.meta.resolver.media;

import com.socials.extractor.browser.BrowserCapture;
import com.socials.extractor.model.MediaResult;
import com.socials.extractor.platforms.meta.resolver.browser.BrowserCaptureResolver;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(3)
public class MetaHtmlMetadataResolver implements BrowserCaptureResolver {

    @Override
    public boolean supports(BrowserCapture capture) {
        return capture != null
                && capture.getHtml() != null
                && !capture.getHtml().isBlank();
    }

    @Override
    public MediaResult resolve(BrowserCapture capture) {
        Document document = Jsoup.parse(capture.getHtml());

        // Extract Duration
        String durationStr = meta(document, "video:duration");
        if (durationStr == null) {
            durationStr = meta(document, "og:video:duration");
        }

        Long duration = null;
        if (durationStr != null && !durationStr.isBlank()) {
            try {
                duration = Long.parseLong(durationStr);
            } catch (Exception ignored) {}
        }

        return MediaResult.builder()
                .title(meta(document, "og:title"))
                .thumbnail(meta(document, "og:image"))
                .duration(duration)
                .build();
    }

    private String meta(Document document, String property) {
        String content = document.select("meta[property=" + property + "]").attr("content");
        if (content.isBlank()) {
            // Fallback for names instead of properties
            content = document.select("meta[name=" + property + "]").attr("content");
        }
        return content.isBlank() ? null : content;
    }
}