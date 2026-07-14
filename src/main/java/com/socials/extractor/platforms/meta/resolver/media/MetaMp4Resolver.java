package com.socials.extractor.platforms.meta.resolver.media;

import com.socials.extractor.browser.BrowserCapture;
import com.socials.extractor.browser.BrowserResponse;
import com.socials.extractor.model.MediaFormat;
import com.socials.extractor.model.MediaResult;
import com.socials.extractor.model.Platform;
import com.socials.extractor.platforms.meta.resolver.browser.BrowserCaptureResolver;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
@Order(2)
public class MetaMp4Resolver implements BrowserCaptureResolver {

    @Override
    public boolean supports(
            BrowserCapture capture
    ) {

        return capture.getResponses()
                .stream()
                .anyMatch(this::isPlayableVideo);

    }

    @Override
    public MediaResult resolve(
            BrowserCapture capture
    ) {

        Map<String, BrowserResponse> videos =
                new LinkedHashMap<>();

        for (BrowserResponse response : capture.getResponses()) {

            if (!isPlayableVideo(response)) {
                continue;
            }

            videos.putIfAbsent(

                    normalize(response.getUrl()),

                    response

            );

        }

        if (videos.isEmpty()) {

            throw new IllegalStateException(
                    "No playable Meta video found."
            );

        }

        List<BrowserResponse> responses =
                new ArrayList<>(videos.values());

        BrowserResponse best =

                responses.stream()

                        .max(

                                Comparator.comparingLong(
                                        this::score
                                )

                        )

                        .orElse(responses.getFirst());

        List<MediaFormat> formats =

                responses.stream()

                        .map(this::toFormat)

                        .toList();

        return MediaResult.builder()

                .platform(
                        resolvePlatform(capture)
                )

                .url(
                        best.getUrl()
                )

                .formats(
                        new ArrayList<>(formats)
                )

                .build();

    }

    /**
     * Accept only actual video responses.
     */
    private boolean isPlayableVideo(
            BrowserResponse response
    ) {

        String url =
                response.getUrl();

        if (url == null) {
            return false;
        }

        String contentType =
                response.getContentType();

        if (contentType == null
                || !contentType.toLowerCase().startsWith("video/")) {

            return false;

        }

        url = url.toLowerCase();

        return url.contains(".mp4")
                || url.contains("video.")
                || url.contains("tag=dash");

    }

    /**
     * Remove volatile query parameters so the same
     * video isn't stored multiple times.
     */
    private String normalize(
            String url
    ) {

        if (url == null) {
            return "";
        }

        return url

                .replaceAll("[?&]bytestart=\\d+", "")

                .replaceAll("[?&]byteend=\\d+", "")

                .replaceAll("[?&]_nc_gid=[^&]+", "")

                .replaceAll("[?&]oh=[^&]+", "")

                .replaceAll("[?&]oe=[^&]+", "");

    }

    /**
     * Prefer progressive videos, then DASH,
     * then larger files.
     */
    private long score(
            BrowserResponse response
    ) {

        String url =
                response.getUrl()
                        .toLowerCase();

        long score =
                contentLength(response);

        if (url.contains("progressive")) {

            score += 1_000_000;

        }

        if (url.contains("dash_h264")) {

            score += 500_000;

        }

        if (url.contains("dash_av1")) {

            score += 250_000;

        }

        return score;

    }

    private long contentLength(
            BrowserResponse response
    ) {

        try {

            return Long.parseLong(

                    response.getHeaders()

                            .getOrDefault(
                                    "content-length",
                                    "0"
                            )

            );

        }

        catch (Exception ignored) {

            return 0L;

        }

    }

    private MediaFormat toFormat(
            BrowserResponse response
    ) {

        return MediaFormat.builder()

                .url(
                        response.getUrl()
                )

                .mimeType(
                        response.getContentType()
                )

                .contentLength(
                        contentLength(response)
                )

                .build();

    }

    private Platform resolvePlatform(
            BrowserCapture capture
    ) {

        String url =
                capture.getFinalUrl();

        if (url == null) {

            return Platform.INSTAGRAM;

        }

        url = url.toLowerCase();

        if (url.contains("facebook.com")
                || url.contains("fb.watch")) {

            return Platform.FACEBOOK;

        }

        return Platform.INSTAGRAM;

    }

}