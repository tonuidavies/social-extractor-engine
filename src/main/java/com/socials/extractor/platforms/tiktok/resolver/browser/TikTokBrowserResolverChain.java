package com.socials.extractor.platforms.tiktok.resolver.browser;

import com.socials.extractor.browser.BrowserCapture;
import com.socials.extractor.model.MediaFormat;
import com.socials.extractor.model.MediaResult;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class TikTokBrowserResolverChain {

    private final List<TikTokBrowserCaptureResolver> resolvers;

    public MediaResult resolve(BrowserCapture capture) {
        MediaResult result = MediaResult.builder().formats(new ArrayList<>()).build();

        List<TikTokBrowserCaptureResolver> sortedResolvers = resolvers.stream()
                .sorted(AnnotationAwareOrderComparator.INSTANCE)
                .toList();

        for (TikTokBrowserCaptureResolver resolver : sortedResolvers) {
            if (resolver.supports(capture)) {
                MediaResult partial = resolver.resolve(capture);
                merge(result, partial);
            }
        }
        return result.getUrl() != null || !result.getFormats().isEmpty() ? result : null;
    }

    private void merge(MediaResult target, MediaResult source) {
        if (source == null) return;
        if (target.getPlatform() == null) target.setPlatform(source.getPlatform());
        if (target.getTitle() == null) target.setTitle(source.getTitle());
        if (target.getThumbnail() == null) target.setThumbnail(source.getThumbnail());
        if (target.getUrl() == null) target.setUrl(source.getUrl());
        if (target.getDuration() == null) target.setDuration(source.getDuration());
        
        if (source.getFormats() != null) {
            for (MediaFormat sourceFormat : source.getFormats()) {
                boolean exists = target.getFormats().stream()
                        .anyMatch(f -> f.getUrl().equals(sourceFormat.getUrl()));
                if (!exists) {
                    target.getFormats().add(sourceFormat);
                }
            }
        }
    }
}