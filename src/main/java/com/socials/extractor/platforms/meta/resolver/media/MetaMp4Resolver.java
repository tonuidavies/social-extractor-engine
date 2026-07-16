package com.socials.extractor.platforms.meta.resolver.media;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.socials.extractor.browser.BrowserCapture;
import com.socials.extractor.browser.BrowserResponse;
import com.socials.extractor.model.MediaFormat;
import com.socials.extractor.model.MediaResult;
import com.socials.extractor.model.Platform;
import com.socials.extractor.platforms.meta.resolver.browser.BrowserCaptureResolver;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@Order(2)
public class MetaMp4Resolver implements BrowserCaptureResolver {

    private final ObjectMapper mapper = new ObjectMapper();
    private static final Pattern EFG_PATTERN = Pattern.compile("[?&]efg=([^&]+)");

    @Override
    public boolean supports(BrowserCapture capture) {
        return capture.getResponses()
                .stream()
                .anyMatch(this::isPlayableVideo);
    }

    @Override
    public MediaResult resolve(BrowserCapture capture) {

        Map<String, BrowserResponse> videos = new LinkedHashMap<>();

        for (BrowserResponse response : capture.getResponses()) {
            if (!isPlayableVideo(response)) {
                continue;
            }
            videos.putIfAbsent(normalize(response.getUrl()), response);
        }

        if (videos.isEmpty()) {
            throw new IllegalStateException("No playable Meta video found.");
        }

        List<BrowserResponse> responses = new ArrayList<>(videos.values());

        BrowserResponse best = responses.stream()
                .max(Comparator.comparingLong(this::score))
                .orElse(responses.getFirst());

        List<MediaFormat> formats = responses.stream()
                .map(this::toFormat)
                .toList();

        // Extract duration from the hidden EFG parameter
        Long duration = null;
        JsonNode efgNode = decodeEfg(best.getUrl());
        if (efgNode != null && efgNode.has("duration_s")) {
            duration = efgNode.get("duration_s").asLong();
        }

        return MediaResult.builder()
                .platform(resolvePlatform(capture))
                .url(best.getUrl())
                .duration(duration)
                .formats(new ArrayList<>(formats))
                .build();
    }

    private MediaFormat toFormat(BrowserResponse response) {
        String url = response.getUrl();
        String quality = null;

        if (url != null) {
            String lowerUrl = url.toLowerCase();
            if (lowerUrl.contains("1080p")) quality = "1080p";
            else if (lowerUrl.contains("720p")) quality = "720p";
            else if (lowerUrl.contains("480p")) quality = "480p";
            else if (lowerUrl.contains("360p")) quality = "360p";

            // If quality isn't in plain text (like Instagram), decode the hidden EFG JSON
            if (quality == null) {
                JsonNode efgNode = decodeEfg(url);
                if (efgNode != null && efgNode.has("vencode_tag")) {
                    String vencode = efgNode.get("vencode_tag").asText().toLowerCase();
                    if (vencode.contains("1080")) quality = "1080p";
                    else if (vencode.contains("720")) quality = "720p";
                    else if (vencode.contains("480")) quality = "480p";
                    else if (vencode.contains("360")) quality = "360p";
                }
            }
        }

        return MediaFormat.builder()
                .url(url)
                .mimeType(response.getContentType())
                .contentLength(contentLength(response))
                .quality(quality)
                .build();
    }

    /**
     * Decodes the Base64 JSON payload hidden inside Meta's "efg" URL parameter.
     */
    private JsonNode decodeEfg(String url) {
        if (url == null) return null;
        try {
            Matcher m = EFG_PATTERN.matcher(url);
            if (m.find()) {
                String efg = URLDecoder.decode(m.group(1), StandardCharsets.UTF_8);

                // Fix Base64 padding if Meta truncated it
                while (efg.length() % 4 != 0) {
                    efg += "=";
                }

                byte[] decoded;
                try {
                    decoded = Base64.getUrlDecoder().decode(efg);
                } catch (IllegalArgumentException e) {
                    decoded = Base64.getDecoder().decode(efg);
                }

                return mapper.readTree(new String(decoded, StandardCharsets.UTF_8));
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private boolean isPlayableVideo(BrowserResponse response) {
        String url = response.getUrl();
        if (url == null) return false;

        String contentType = response.getContentType();
        if (contentType == null || !contentType.toLowerCase().startsWith("video/")) {
            return false;
        }

        url = url.toLowerCase();
        return url.contains(".mp4") || url.contains("video.") || url.contains("tag=dash");
    }

    private String normalize(String url) {
        if (url == null) return "";
        return url
                .replaceAll("[?&]bytestart=\\d+", "")
                .replaceAll("[?&]byteend=\\d+", "")
                .replaceAll("[?&]_nc_gid=[^&]+", "")
                .replaceAll("[?&]oh=[^&]+", "")
                .replaceAll("[?&]oe=[^&]+", "");
    }

    private long score(BrowserResponse response) {
        String url = response.getUrl().toLowerCase();
        long score = contentLength(response);

        if (url.contains("progressive")) score += 1_000_000;
        if (url.contains("dash_h264")) score += 500_000;
        if (url.contains("dash_av1")) score += 250_000;

        return score;
    }

    private long contentLength(BrowserResponse response) {
        try {
            return Long.parseLong(response.getHeaders().getOrDefault("content-length", "0"));
        } catch (Exception ignored) {
            return 0L;
        }
    }

    private Platform resolvePlatform(BrowserCapture capture) {
        String url = capture.getFinalUrl();
        if (url == null) return Platform.INSTAGRAM;
        url = url.toLowerCase();
        if (url.contains("facebook.com") || url.contains("fb.watch")) return Platform.FACEBOOK;
        return Platform.INSTAGRAM;
    }
}