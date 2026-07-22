package com.socials.extractor.api;

import com.socials.extractor.core.ExtractorEngine;
import com.socials.extractor.model.ExtractionRequest;
import com.socials.extractor.model.ExtractionResponse;
import com.socials.extractor.proxy.ProxyUrlRewriter;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/extract")
@RequiredArgsConstructor
@CrossOrigin("*")
public class ExtractorController {

    private final ExtractorEngine extractorEngine;
    private final ProxyUrlRewriter proxyUrlRewriter;

    /**
     * Optional fixed public base URL, e.g. https://api.example.com — set this
     * when the service sits behind a reverse proxy / load balancer so the
     * generated download links point at the public host instead of the
     * internal one. When blank, the base URL is derived from the request.
     */
    @Value("${media.proxy.public-base-url:}")
    private String configuredBaseUrl;

    @PostMapping
    public Mono<ExtractionResponse> extract(@Valid @RequestBody ExtractionRequest request,
                                            ServerHttpRequest httpRequest) {

        String baseUrl = resolveBaseUrl(httpRequest);

        return extractorEngine.extract(request)
                .map(response -> rewrite(response, baseUrl));
    }

    private ExtractionResponse rewrite(ExtractionResponse response, String baseUrl) {
        if (response != null && response.getMedia() != null) {
            proxyUrlRewriter.rewrite(response.getMedia(), baseUrl);
        }
        return response;
    }

    private String resolveBaseUrl(ServerHttpRequest request) {
        if (StringUtils.hasText(configuredBaseUrl)) {
            return trimTrailingSlash(configuredBaseUrl);
        }
        // Honour X-Forwarded-* if present (Spring populates the URI when the
        // ForwardedHeaderTransformer is enabled); otherwise use the raw URI.
        URI uri = request.getURI();
        String scheme = uri.getScheme();
        String authority = uri.getAuthority(); // host[:port]
        return scheme + "://" + authority;
    }

    private String trimTrailingSlash(String url) {
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }
}