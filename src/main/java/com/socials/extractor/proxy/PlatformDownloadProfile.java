package com.socials.extractor.proxy;

import com.socials.extractor.model.Platform;
import org.springframework.http.HttpHeaders;

/**
 * Per-platform HTTP header profile used when the backend re-fetches a CDN media
 * URL on behalf of the client.
 *
 * <p>The whole reason this class exists: social CDNs protect their signed media
 * URLs with hot-link / referer checks. The signature in the URL only proves the
 * URL wasn't tampered with — the CDN still refuses to serve the bytes unless the
 * request *looks like* it came from the origin site (correct {@code Referer},
 * {@code Origin}, {@code User-Agent} and Fetch-Metadata headers).
 *
 * <p>A browser clicking the raw URL sends none of these, so it gets
 * {@code Access Denied}. Replaying them here is what makes the download work.
 *
 * <p>Values are intentionally described by <em>role</em> (the origin site,
 * a modern desktop Chrome UA) rather than pinned to a brittle build number, so
 * the profile keeps working when platforms rotate their signing internals.
 */
public enum PlatformDownloadProfile {

    TIKTOK(
            "https://www.tiktok.com/",
            "https://www.tiktok.com"
    ),
    INSTAGRAM(
            "https://www.instagram.com/",
            "https://www.instagram.com"
    ),
    FACEBOOK(
            "https://www.facebook.com/",
            "https://www.facebook.com"
    ),
    YOUTUBE(
            "https://www.youtube.com/",
            "https://www.youtube.com"
    ),
    GENERIC(
            null,
            null
    );

    /**
     * A current, realistic desktop Chrome UA. Kept in one place so it is trivial
     * to bump. CDNs care that this is a plausible browser, not that it is exact.
     */
    public static final String DEFAULT_USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
                    + "(KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36";

    private final String referer;
    private final String origin;

    PlatformDownloadProfile(String referer, String origin) {
        this.referer = referer;
        this.origin = origin;
    }

    public static PlatformDownloadProfile forPlatform(Platform platform) {
        if (platform == null) {
            return GENERIC;
        }
        return switch (platform) {
            case TIKTOK -> TIKTOK;
            case INSTAGRAM -> INSTAGRAM;
            case FACEBOOK -> FACEBOOK;
            case YOUTUBE -> YOUTUBE;
            default -> GENERIC;
        };
    }

    /**
     * Best-effort guess of the profile from the target CDN host, used as a
     * fallback when the caller did not pass an explicit platform.
     */
    public static PlatformDownloadProfile forHost(String host) {
        if (host == null) {
            return GENERIC;
        }
        String h = host.toLowerCase();
        if (h.contains("tiktok") || h.contains("ttwstatic") || h.contains("muscdn") || h.contains("ibyteimg")) {
            return TIKTOK;
        }
        if (h.contains("cdninstagram") || h.contains("instagram")) {
            return INSTAGRAM;
        }
        if (h.contains("fbcdn") || h.contains("facebook") || h.contains("fbsbx")) {
            return FACEBOOK;
        }
        if (h.contains("googlevideo") || h.contains("youtube") || h.contains("ytimg")) {
            return YOUTUBE;
        }
        return GENERIC;
    }

    /**
     * Apply the base disguise headers for this platform. The proxy layers Range,
     * Cookie and per-request overrides on top of these.
     */
    public void apply(HttpHeaders headers, String userAgentOverride) {

        String ua = (userAgentOverride != null && !userAgentOverride.isBlank())
                ? userAgentOverride
                : DEFAULT_USER_AGENT;

        headers.set(HttpHeaders.USER_AGENT, ua);
        headers.set(HttpHeaders.ACCEPT, "*/*");
        headers.set(HttpHeaders.ACCEPT_LANGUAGE, "en-US,en;q=0.9");

        if (referer != null) {
            headers.set(HttpHeaders.REFERER, referer);
        }
        if (origin != null) {
            headers.set(HttpHeaders.ORIGIN, origin);
        }

        // Fetch-Metadata: browsers send these on media sub-resource requests.
        // Several CDNs (TikTok in particular) reject requests that lack them.
        headers.set("Sec-Fetch-Dest", "video");
        headers.set("Sec-Fetch-Mode", "no-cors");
        headers.set("Sec-Fetch-Site", "same-site");
        headers.set("Accept-Encoding", "identity");
    }

    public String referer() {
        return referer;
    }

    public String origin() {
        return origin;
    }
}
