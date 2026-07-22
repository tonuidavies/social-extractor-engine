package com.socials.extractor.proxy;

import com.socials.extractor.model.Platform;
import lombok.RequiredArgsConstructor;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/**
 * Streaming download endpoint. Clients receive URLs pointing here (produced by
 * {@link ProxyUrlRewriter}); this controller re-fetches the underlying CDN
 * resource with the correct origin headers and streams it straight through.
 *
 * <p>Example URL handed to the client:
 * <pre>
 *   http://192.168.100.12:8080/api/v1/download?u=aHR0cHM6Ly92MTYt...&p=TIKTOK&n=clip.mp4
 * </pre>
 */
@RestController
@RequestMapping("/api/v1/download")
@RequiredArgsConstructor
@CrossOrigin("*")
public class MediaProxyController {

    private final MediaProxyService proxyService;

    @GetMapping
    public Mono<Void> download(
            @RequestParam("u") String encodedUrl,
            @RequestParam(value = "p", required = false) String platform,
            @RequestParam(value = "n", required = false) String filename,
            ServerHttpRequest request,
            ServerHttpResponse response) {

        Platform p = parsePlatform(platform);
        return proxyService.stream(encodedUrl, p, filename, request, response);
    }

    private Platform parsePlatform(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Platform.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
