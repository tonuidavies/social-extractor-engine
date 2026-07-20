package com.socials.extractor.platforms.tiktok.resolver.media;

import com.socials.extractor.browser.BrowserCapture;
import com.socials.extractor.browser.BrowserResponse;
import com.socials.extractor.model.MediaFormat;
import com.socials.extractor.model.MediaResult;
import com.socials.extractor.model.Platform;
import com.socials.extractor.platforms.tiktok.resolver.browser.TikTokBrowserCaptureResolver;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
@Order(2)
public class TikTokMp4Resolver implements TikTokBrowserCaptureResolver {

    @Override
    public boolean supports(BrowserCapture capture) {
        return capture.getResponses().stream().anyMatch(this::isPlayableVideo);
    }

    @Override
    public MediaResult resolve(BrowserCapture capture) {
        Map<String, BrowserResponse> videos = new LinkedHashMap<>();

        for (BrowserResponse response : capture.getResponses()) {
            if (isPlayableVideo(response)) {
                videos.putIfAbsent(normalize(response.getUrl()), response);
            }
        }

        if (videos.isEmpty()) {
            return MediaResult.builder().formats(new ArrayList<>()).build();
        }

        List<BrowserResponse> responses = new ArrayList<>(videos.values());
        BrowserResponse best = responses.stream()
                .max(Comparator.comparingLong(this::contentLength))
                .orElse(responses.getFirst());

        List<MediaFormat> formats = responses.stream()
                .map(this::toFormat)
                .toList();

        return MediaResult.builder()
                .platform(Platform.TIKTOK)
                .url(best.getUrl())
                .formats(new ArrayList<>(formats))
                .build();
    }

    private boolean isPlayableVideo(BrowserResponse response) {
        String url = response.getUrl();
        if (url == null) return false;

        String contentType = response.getContentType();
        if (contentType != null && contentType.toLowerCase().startsWith("video/")) return true;
        
        url = url.toLowerCase();
        return url.contains("/video/tos/") || url.contains("mime_type=video_mp4");
    }

    private String normalize(String url) {
        return url == null ? "" : url.replaceAll("&range=[^&]+", "").replaceAll("&rn=[^&]+", "");
    }

    private long contentLength(BrowserResponse response) {
        try {
            return Long.parseLong(response.getHeaders().getOrDefault("content-length", "0"));
        } catch (Exception ignored) {
            return 0L;
        }
    }

    private MediaFormat toFormat(BrowserResponse response) {
        return MediaFormat.builder()
                .url(response.getUrl())
                .mimeType(response.getContentType() != null ? response.getContentType() : "video/mp4")
                .contentLength(contentLength(response))
                .quality("Network Capture")
                .build();
    }
}