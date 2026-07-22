package com.socials.extractor.proxy;

import org.springframework.stereotype.Component;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Short-lived store of the cookies and User-Agent that were live in the browser
 * session at the moment a media URL was captured.
 *
 * <p>Some signed CDN URLs (TikTok's {@code tk=tt_chain_token} is the classic
 * example) are validated against a cookie that existed in the capturing session.
 * The proxy replays those cookies when it re-fetches, which is what lets an
 * otherwise session-bound URL succeed.
 *
 * <p>Entries are keyed by CDN host and expire automatically, so this never grows
 * unbounded and never leaks a user's cookies for longer than a download needs.
 * If nothing was registered the proxy still works via Referer/UA alone — this is
 * a robustness enhancement, not a hard dependency.
 */
@Component
public class CaptureSessionRegistry {

    /** How long a captured cookie set stays usable. CDN URLs expire quickly anyway. */
    private static final Duration TTL = Duration.ofMinutes(15);

    private final Map<String, Entry> byHost = new ConcurrentHashMap<>();

    /**
     * Register the session context that produced media on {@code cdnHost}.
     *
     * @param cdnHost   host of the media URL (e.g. {@code v16-webapp-prime.tiktok.com})
     * @param cookieHeader raw {@code Cookie} header string (may be null/blank)
     * @param userAgent the UA used during capture (may be null)
     */
    public void register(String cdnHost, String cookieHeader, String userAgent) {
        if (cdnHost == null || cdnHost.isBlank()) {
            return;
        }
        purgeExpired();
        byHost.put(cdnHost.toLowerCase(),
                new Entry(cookieHeader, userAgent, Instant.now().plus(TTL)));
    }

    /** Convenience overload that accepts a full media URL. */
    public void registerForUrl(String mediaUrl, String cookieHeader, String userAgent) {
        String host = hostOf(mediaUrl);
        if (host != null) {
            register(host, cookieHeader, userAgent);
        }
    }

    public String cookieHeader(String cdnHost) {
        Entry e = live(cdnHost);
        return e == null ? null : e.cookieHeader;
    }

    public String userAgent(String cdnHost) {
        Entry e = live(cdnHost);
        return e == null ? null : e.userAgent;
    }

    private Entry live(String cdnHost) {
        if (cdnHost == null) {
            return null;
        }
        Entry e = byHost.get(cdnHost.toLowerCase());
        if (e == null) {
            return null;
        }
        if (Instant.now().isAfter(e.expiresAt)) {
            byHost.remove(cdnHost.toLowerCase());
            return null;
        }
        return e;
    }

    private void purgeExpired() {
        Instant now = Instant.now();
        byHost.entrySet().removeIf(en -> now.isAfter(en.getValue().expiresAt));
    }

    public static String hostOf(String url) {
        try {
            return URI.create(url).getHost();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Build a {@code Cookie} header string from a name/value map — handy when the
     * capture layer holds cookies as a map rather than a raw header.
     */
    public static String toCookieHeader(Map<String, String> cookies) {
        if (cookies == null || cookies.isEmpty()) {
            return null;
        }
        return cookies.entrySet().stream()
                .map(e -> e.getKey() + "=" + e.getValue())
                .collect(Collectors.joining("; "));
    }

    private record Entry(String cookieHeader, String userAgent, Instant expiresAt) {
    }
}
