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
 * <p>Because the same backend that captured the URL is the one re-fetching it,
 * the request originates from the same IP and can carry the same cookies — the
 * two things a session-bound signature checks.
 *
 * <p>Bytes are streamed (never fully buffered), and the client's {@code Range}
 * header is forwarded upstream, so seeking and resumable downloads work.
 */
@Service
@RequiredArgsConstructor
public class MediaProxyService {

    private final WebClient mediaProxyWebClient;
    private final CaptureSessionRegistry sessions;

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

    /**
     * @param encodedUrl base64url-encoded original CDN URL
     * @param platform   platform hint (may be null; host is used as fallback)
     * @param filename   optional download filename for Content-Disposition
     */
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

        final String host = targetUri.getHost();

        // Resolve the profile: explicit platform wins, else infer from host.
        PlatformDownloadProfile profile = (platform != null && platform != Platform.UNKNOWN)
                ? PlatformDownloadProfile.forPlatform(platform)
                : PlatformDownloadProfile.forHost(host);

        // Replay the capture session's UA/cookies if we have them for this host.
        String capturedUa = sessions.userAgent(host);
        String capturedCookie = sessions.cookieHeader(host);

        return mediaProxyWebClient.get()
                .uri(targetUri)
                .headers(h -> {
                    profile.apply(h, capturedUa);

                    if (StringUtils.hasText(capturedCookie)) {
                        h.set(HttpHeaders.COOKIE, capturedCookie);
                    }

                    // Forward the client's Range so seeking / partial downloads work.
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

                    // Stream the body straight through — no full-buffer in memory.
                    return outbound.writeWith(upstream.bodyToFlux(DataBuffer.class))
                            .onErrorResume(err -> {
                                // Client disconnected mid-stream, etc. Nothing to salvage.
                                return Mono.empty();
                            });
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
