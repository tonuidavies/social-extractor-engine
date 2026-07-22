package com.socials.extractor.platforms.tiktok.service;

import com.socials.extractor.browser.BrowserCapture;
import com.socials.extractor.browser.BrowserRequest;
import com.socials.extractor.model.MediaResult;
import com.socials.extractor.platforms.tiktok.resolver.browser.TikTokBrowserResolverChain;
import com.socials.extractor.proxy.CaptureSessionRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TikTokMediaService {

    private final TikTokBrowserResolverChain resolverChain;
    private final CaptureSessionRegistry sessions;

    private static final String DEFAULT_UA =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
                    + "(KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36";

    public MediaResult resolve(BrowserCapture capture) {

        MediaResult result = resolverChain.resolve(capture);

        registerCaptureSession(result, capture);

        return result;
    }

    /**
     * Store the cookie header + UA from the capture, keyed by the host of every
     * media URL in the result, so the proxy can look them up by host later.
     */
    private void registerCaptureSession(MediaResult result, BrowserCapture capture) {
        if (result == null || capture == null) {
            return;
        }

        String cookie = extractCookieHeader(capture);
        String userAgent = extractUserAgent(capture);

        if (cookie == null || cookie.isBlank()) {
            return; // nothing worth replaying
        }

        if (result.getUrl() != null) {
            sessions.registerForUrl(result.getUrl(), cookie, userAgent);
        }
        if (result.getFormats() != null) {
            result.getFormats().forEach(f -> {
                if (f != null && f.getUrl() != null) {
                    sessions.registerForUrl(f.getUrl(), cookie, userAgent);
                }
            });
        }
    }

    /**
     * Pull the raw {@code Cookie} header the browser sent to TikTok during
     * capture. Mirrors the logic already proven in TikTokDownloadService: prefer
     * the most recent tiktok.com request that carries a real cookie.
     */
    private String extractCookieHeader(BrowserCapture capture) {
        if (capture.getRequests() == null) {
            return null;
        }
        for (int i = capture.getRequests().size() - 1; i >= 0; i--) {
            BrowserRequest request = capture.getRequests().get(i);
            if (request.getUrl() != null
                    && request.getUrl().contains("tiktok.com")
                    && request.getHeaders() != null) {
                for (var entry : request.getHeaders().entrySet()) {
                    if (entry.getKey().equalsIgnoreCase("cookie")
                            && entry.getValue() != null
                            && entry.getValue().length() > 20) {
                        return entry.getValue();
                    }
                }
            }
        }
        return null;
    }

    private String extractUserAgent(BrowserCapture capture) {
        if (capture.getRequests() == null) {
            return DEFAULT_UA;
        }
        for (BrowserRequest request : capture.getRequests()) {
            if (request.getHeaders() != null) {
                for (var entry : request.getHeaders().entrySet()) {
                    if (entry.getKey().equalsIgnoreCase("user-agent")
                            && entry.getValue() != null
                            && !entry.getValue().isBlank()) {
                        return entry.getValue();
                    }
                }
            }
        }
        return DEFAULT_UA;
    }
}
