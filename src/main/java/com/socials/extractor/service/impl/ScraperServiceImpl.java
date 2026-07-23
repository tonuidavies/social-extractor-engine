package com.socials.extractor.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.socials.extractor.service.ScraperService;
import org.apache.http.HttpHost;
import org.apache.http.client.fluent.Executor;
import org.apache.http.client.fluent.Request;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class ScraperServiceImpl implements ScraperService {

    private static final Logger log = LoggerFactory.getLogger(ScraperServiceImpl.class);
    private static final String YT_DLP_PATH = "yt-dlp";
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${webshare.proxy.enabled:false}")
    private boolean proxyEnabled;

    @Value("${webshare.proxy.host:p.webshare.io}")
    private String proxyHost;

    @Value("${webshare.proxy.port:80}")
    private int proxyPort;

    @Value("${proxy.username}")
    private String proxyUsername;

    @Value("${proxy.password}")
    private String proxyPassword;

    @Value("${ytdlp.cookies.file:}")
    private String cookiesFilePath;

    @Value("${ytdlp.impersonate:false}")
    private boolean impersonateBrowser;

    @Override
    public Map<String, String> getVideoInfo(String userUrl) throws Exception {
        return extractWithYtDlpWithRetry(userUrl);
    }

    private Map<String, String> extractWithYtDlpWithRetry(String userUrl) throws Exception {
        int maxRetries = 2;
        int attempt = 0;
        Exception lastException = null;

        while (attempt <= maxRetries) {
            try {
                return extractWithYtDlp(userUrl);
            } catch (Exception e) {
                lastException = e;
                attempt++;
                if (attempt <= maxRetries) {
                    log.warn("Extraction failed (attempt {}/{}), retrying in {} ms... Error: {}",
                            attempt, maxRetries, attempt * 1000, e.getMessage());
                    Thread.sleep(attempt * 1000L);
                }
            }
        }
        throw new RuntimeException("Extraction failed after " + (maxRetries + 1) + " attempts", lastException);
    }

    private Map<String, String> extractWithYtDlp(String userUrl) throws Exception {
        log.info("Extracting video info via yt-dlp for: {}", userUrl);

        if (proxyEnabled) {
            validateProxy();
        } else {
            log.warn("⚠️ Proxy DISABLED – your real IP will be exposed!");
        }

        ProcessBuilder pb = new ProcessBuilder(buildCommand(userUrl, true));
        pb.redirectErrorStream(true);

        if (proxyEnabled) {
            String proxyUrlWithAuth = buildProxyUrl();
            Map<String, String> env = pb.environment();
            env.put("HTTP_PROXY", proxyUrlWithAuth);
            env.put("HTTPS_PROXY", proxyUrlWithAuth);
            env.put("http_proxy", proxyUrlWithAuth);
            env.put("https_proxy", proxyUrlWithAuth);
        }

        Process process = pb.start();
        String stdout;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            stdout = reader.lines().collect(Collectors.joining("\n"));
        }

        if (!process.waitFor(30, TimeUnit.SECONDS)) {
            process.destroyForcibly();
            throw new RuntimeException("yt-dlp timed out after 30 seconds.");
        }

        if (process.exitValue() != 0) {
            log.error("yt-dlp failed:\n{}", stdout);
            throw new RuntimeException("Extraction failed. The platform might require cookies or the link is private.");
        }

        JsonNode node = objectMapper.readTree(stdout);
        Map<String, String> response = new HashMap<>();
        response.put("title", node.has("title") ? node.get("title").asText() : "Unknown Title");
        response.put("thumbnail", node.has("thumbnail") ? node.get("thumbnail").asText() : "");

        String downloadUrl = node.has("url") && !node.get("url").isNull() ? node.get("url").asText() : "";
        if (downloadUrl.isBlank() && node.has("requested_downloads")) {
            downloadUrl = node.get("requested_downloads").get(0).get("url").asText();
        }

        response.put("downloadUrl", downloadUrl);
        response.put("originalUrl", userUrl);
        log.info("✅ Extraction successful: {}", response.get("title"));
        return response;
    }

    @Override
    public void streamVideoBytes(String userUrl, OutputStream out) throws Exception {
        log.info("Starting byte stream for: {}", userUrl);

        ProcessBuilder pb = new ProcessBuilder(buildCommand(userUrl, false));
        pb.redirectErrorStream(false);

        if (proxyEnabled) {
            String proxyUrlWithAuth = buildProxyUrl();
            pb.environment().put("HTTP_PROXY", proxyUrlWithAuth);
            pb.environment().put("HTTPS_PROXY", proxyUrlWithAuth);
        }

        Process process = pb.start();

        try (InputStream in = process.getInputStream()) {
            in.transferTo(out);
            out.flush();
        }

        if (!process.waitFor(60, TimeUnit.SECONDS)) {
            process.destroyForcibly();
            throw new RuntimeException("Stream timed out");
        }
        if (process.exitValue() != 0) {
            throw new RuntimeException("Stream extraction failed.");
        }
    }

    private String[] buildCommand(String url, boolean dumpJson) {
        java.util.List<String> cmd = new java.util.ArrayList<>();
        cmd.add(YT_DLP_PATH);
        cmd.add("--no-warnings");
        cmd.add("--no-playlist");

        // Force close connection after each request to get a new IP from rotating proxy
        cmd.add("--add-header");
        cmd.add("Connection: close");

        if (dumpJson) {
            cmd.add("--dump-json");
            cmd.add("-f");
            cmd.add("best[ext=mp4]");
        } else {
            cmd.add("-q");
            cmd.add("-f");
            cmd.add("b[ext=mp4]/best/b");
            cmd.add("-o");
            cmd.add("-");
        }

        String lowerUrl = url.toLowerCase();
        if (lowerUrl.contains("tiktok.com")) {
            cmd.add("--add-header");
            cmd.add("User-Agent: Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");
            cmd.add("--add-header");
            cmd.add("Referer: https://www.tiktok.com/");
        } else if (lowerUrl.contains("instagram.com")) {
            cmd.add("--add-header");
            cmd.add("Referer: https://www.instagram.com/");
        }

        if (impersonateBrowser) {
            cmd.add("--impersonate");
            cmd.add("chrome");
        }

        if (proxyEnabled) {
            cmd.add("--proxy");
            cmd.add(buildProxyUrl());
            cmd.add("--no-check-certificate");
            cmd.add("--socket-timeout");
            cmd.add("30");
        }

        if (cookiesFilePath != null && !cookiesFilePath.isBlank()) {
            Path cookiesPath = Paths.get(cookiesFilePath);
            if (Files.exists(cookiesPath) && Files.isReadable(cookiesPath)) {
                cmd.add("--cookies");
                cmd.add(cookiesFilePath);
            }
        }

        cmd.add(url);
        return cmd.toArray(new String[0]);
    }

    private void validateProxy() {
        if (proxyUsername == null || proxyUsername.isBlank() || proxyPassword == null || proxyPassword.isBlank()) {
            throw new RuntimeException("Proxy username/password missing");
        }
        HttpHost proxy = new HttpHost(proxyHost, proxyPort, "http");
        try {
            String response = Executor.newInstance()
                    .auth(proxy, proxyUsername, proxyPassword)
                    .execute(Request.Get("https://ipv4.webshare.io/").viaProxy(proxy))
                    .returnContent().asString();
            log.info("✅ Proxy validation OK – outgoing IP: {}", response.trim());
        } catch (Exception e) {
            log.warn("Proxy validation failed: {}", e.getMessage());
        }
    }

    private String buildProxyUrl() {
        String encodedUser = URLEncoder.encode(proxyUsername, StandardCharsets.UTF_8);
        String encodedPass = URLEncoder.encode(proxyPassword, StandardCharsets.UTF_8);
        return String.format("http://%s:%s@%s:%d", encodedUser, encodedPass, proxyHost, proxyPort);
    }
}