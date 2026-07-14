package com.socials.extractor.platforms.meta.resolver.browser;

import com.socials.extractor.browser.BrowserCapture;
import com.socials.extractor.model.MediaResult;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class BrowserResolverChain {

    private final List<BrowserCaptureResolver> resolvers;

    public MediaResult resolve(
            BrowserCapture capture
    ) {

        MediaResult result =
                MediaResult.builder()
                        .build();

        for (BrowserCaptureResolver resolver :

                resolvers.stream()

                        .sorted(
                                AnnotationAwareOrderComparator.INSTANCE
                        )

                        .toList()) {

            if (!resolver.supports(capture)) {
                continue;
            }

            MediaResult partial =
                    resolver.resolve(capture);

            merge(result, partial);

        }

        if (result.getUrl() == null
                && result.getThumbnail() == null
                && result.getTitle() == null) {

            throw new IllegalStateException(
                    "No BrowserCaptureResolver produced a result."
            );

        }

        return result;

    }

    private void merge(
            MediaResult target,
            MediaResult source
    ) {

        if (source == null) {
            return;
        }

        if (target.getPlatform() == null) {
            target.setPlatform(source.getPlatform());
        }

        if (target.getTitle() == null) {
            target.setTitle(source.getTitle());
        }

        if (target.getThumbnail() == null) {
            target.setThumbnail(source.getThumbnail());
        }

        if (target.getUrl() == null) {
            target.setUrl(source.getUrl());
        }

        if (target.getDuration() == null) {
            target.setDuration(source.getDuration());
        }

        if (source.getFormats() != null
                && !source.getFormats().isEmpty()) {

            if (target.getFormats() == null) {
                target.setFormats(new ArrayList<>());
            }

            target.getFormats().addAll(source.getFormats());

        }

    }

}