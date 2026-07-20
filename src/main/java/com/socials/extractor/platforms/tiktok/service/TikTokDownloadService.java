package com.socials.extractor.platforms.tiktok.service;

import com.microsoft.playwright.APIRequest;
import com.microsoft.playwright.APIRequestContext;
import com.microsoft.playwright.APIResponse;
import com.microsoft.playwright.Playwright;
import com.socials.extractor.browser.BrowserCapture;
import com.socials.extractor.browser.BrowserRequest;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Service
public class TikTokDownloadService {

    public CompletableFuture<InputStream> downloadVideoAsync(String videoUrl, BrowserCapture capture) {
        return CompletableFuture.supplyAsync(() -> {
            try (Playwright playwright = Playwright.create()) {
                
                String userAgent = extractUserAgent(capture);
                String cookies = extractCookies(capture);

                Map<String, String> headers = new HashMap<>();
                headers.put("User-Agent", userAgent);
                headers.put("Referer", "https://www.tiktok.com/");
                if (cookies != null && !cookies.isBlank()) {
                    headers.put("Cookie", cookies);
                }

                APIRequestContext apiContext = playwright.request().newContext(
                        new APIRequest.NewContextOptions().setExtraHTTPHeaders(headers)
                );

                APIResponse response = apiContext.get(videoUrl);

                if (!response.ok()) {
                    throw new RuntimeException("Playwright request rejected by CDN. HTTP Status: " + response.status());
                }

                return new ByteArrayInputStream(response.body());
                
            } catch (Exception e) {
                throw new RuntimeException("Failed to download video", e);
            }
        });
    }

    private String extractCookies(BrowserCapture capture) {
        if (capture == null || capture.getRequests() == null) return "";
        for (int i = capture.getRequests().size() - 1; i >= 0; i--) {
            BrowserRequest request = capture.getRequests().get(i);
            if (request.getUrl().contains("tiktok.com") && request.getHeaders() != null) {
                for (var entry : request.getHeaders().entrySet()) {
                    if (entry.getKey().equalsIgnoreCase("cookie") && entry.getValue().length() > 20) {
                        return entry.getValue();
                    }
                }
            }
        }
        return "";
    }

    private String extractUserAgent(BrowserCapture capture) {
        if (capture == null || capture.getRequests() == null) {
            return "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";
        }
        for (BrowserRequest request : capture.getRequests()) {
            if (request.getHeaders() != null) {
                for (var entry : request.getHeaders().entrySet()) {
                    if (entry.getKey().equalsIgnoreCase("user-agent")) return entry.getValue();
                }
            }
        }
        return "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";
    }
}