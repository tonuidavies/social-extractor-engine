package com.socials.extractor.proxy;

import com.socials.extractor.model.Platform;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Set;

/**
 * Streams a remote CDN media resource back to the client, replaying the headers
 * the origin site would have sent, so hot-link-protected and session-bound URLs
 * (TikTok especially) download successfully instead of returning "Access Denied".
 *
 * <p>Reddit special case: v.redd.it videos are DASH (separate audio), so for the
 * REDDIT platform we first try to mux video+audio with ffmpeg (see
 * {@link RedditMuxer}). If there's no audio track, we transparently fall back to
 * the normal single-stream proxy below.
 *
 * <p>Bytes are streamed (never fully buffered), and the client's {@code Range}
 * header is forwarded upstream, so seeking and resumable downloads work.
 */
@Service
@RequiredArgsConstructor
public class MediaProxyService {

    private final WebClient mediaProxyWebClient;
    private final CaptureSessionRegistry sessions;
    private final RedditMuxer redditMuxer;

    /** Response headers we forward from upstream to the client. */
    private static final Set<String> FORWARD_HEADERS = Set.of(
            HttpHeaders.CONTENT_TYPE.toLowerCase(),
            HttpHeaders.CONTENT_LENGTH.toLowerCase(),
            HttpHeaders.CONTENT_RANGE.toLowerCase(),
            HttpHeaders.ACCEPT_RANGES.toLowerCase(),
            HttpHeaders.CONTENT_ENCODING.toLowerCase(),
            HttpHeaders.LAST_MODIFIED.toLowerCase(),
            HttpHeaders.ETAG.toLowerCase(),
            HttpHeaders.CACHE_CONTROL.toLowerCase(),
            HttpHeaders.EXPIRES.toLowerCase()
    );

    public Mono<Void> stream(String encodedUrl,
                             Platform platform,
                             String filename,
                             ServerHttpRequest inbound,
                             ServerHttpResponse outbound) {

        final String targetUrl;
        try {
            targetUrl = decode(encodedUrl);
        } catch (Exception e) {
            outbound.setStatusCode(HttpStatus.BAD_REQUEST);
            return outbound.setComplete();
        }

        final URI targetUri;
        try {
            targetUri = URI.create(targetUrl);
        } catch (Exception e) {
            outbound.setStatusCode(HttpStatus.BAD_REQUEST);
            return outbound.setComplete();
        }

        // Reddit: try muxing audio+video first; fall back to plain proxy if silent.
        if (platform == Platform.REDDIT && redditMuxer.isDashVideo(targetUrl)) {
            return redditMuxer.streamMuxed(targetUrl, filename, outbound)
                    .switchIfEmpty(Mono.defer(() ->
                            proxyStream(targetUrl, targetUri, platform, filename, inbound, outbound)));
        }

        return proxyStream(targetUrl, targetUri, platform, filename, inbound, outbound);
    }

    /** The normal single-URL streaming proxy (unchanged behaviour). */
    private Mono<Void> proxyStream(String targetUrl,
                                   URI targetUri,
                                   Platform platform,
                                   String filename,
                                   ServerHttpRequest inbound,
                                   ServerHttpResponse outbound) {

        final String host = targetUri.getHost();

        PlatformDownloadProfile profile = (platform != null && platform != Platform.UNKNOWN)
                ? PlatformDownloadProfile.forPlatform(platform)
                : PlatformDownloadProfile.forHost(host);

        String capturedUa = sessions.userAgent(host);
        String capturedCookie = sessions.cookieHeader(host);

        return mediaProxyWebClient.get()
                .uri(targetUri)
                .headers(h -> {
                    profile.apply(h, capturedUa);

                    if (StringUtils.hasText(capturedCookie)) {
                        h.set(HttpHeaders.COOKIE, capturedCookie);
                    }

                    List<String> range = inbound.getHeaders().get(HttpHeaders.RANGE);
                    if (range != null && !range.isEmpty()) {
                        h.put(HttpHeaders.RANGE, range);
                    }
                })
                .exchangeToMono(upstream -> {

                    outbound.setStatusCode(upstream.statusCode());

                    HttpHeaders src = upstream.headers().asHttpHeaders();
                    HttpHeaders dst = outbound.getHeaders();
                    src.forEach((name, values) -> {
                        if (FORWARD_HEADERS.contains(name.toLowerCase())) {
                            dst.put(name, values);
                        }
                    });

                    if (StringUtils.hasText(filename)) {
                        dst.setContentDisposition(
                                ContentDisposition.attachment()
                                        .filename(sanitize(filename))
                                        .build());
                    }

                    return outbound.writeWith(upstream.bodyToFlux(DataBuffer.class))
                            .onErrorResume(err -> Mono.empty());
                })
                .onErrorResume(err -> {
                    if (!outbound.isCommitted()) {
                        outbound.setStatusCode(HttpStatus.BAD_GATEWAY);
                    }
                    return outbound.setComplete();
                });
    }

    /* ----------------------------------------------------------------- */

    public static String encode(String url) {
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(url.getBytes(StandardCharsets.UTF_8));
    }

    public static String decode(String encoded) {
        byte[] raw = Base64.getUrlDecoder().decode(encoded);
        return new String(raw, StandardCharsets.UTF_8);
    }

    private static String sanitize(String name) {
        String cleaned = name.replaceAll("[\\r\\n\"\\\\/]", "_").trim();
        return cleaned.isEmpty() ? "video.mp4" : cleaned;
    }
}
