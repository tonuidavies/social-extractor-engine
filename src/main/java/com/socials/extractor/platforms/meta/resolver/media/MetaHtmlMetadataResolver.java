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
public class MetaHtmlMetadataResolver
        implements BrowserCaptureResolver {

    @Override
    public boolean supports(
            BrowserCapture capture
    ) {

        return capture != null
                && capture.getHtml() != null
                && !capture.getHtml().isBlank();

    }

    @Override
    public MediaResult resolve(
            BrowserCapture capture
    ) {

        Document document =
                Jsoup.parse(
                        capture.getHtml()
                );

        return MediaResult.builder()

                .title(
                        meta(
                                document,
                                "og:title"
                        )
                )

                .thumbnail(
                        meta(
                                document,
                                "og:image"
                        )
                )

                .build();

    }

    private String meta(
            Document document,
            String property
    ) {

        return document

                .select(
                        "meta[property=" + property + "]"
                )

                .attr("content");

    }

}