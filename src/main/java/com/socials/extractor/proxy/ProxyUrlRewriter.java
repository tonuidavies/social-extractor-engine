package com.socials.extractor.proxy;

import com.socials.extractor.model.MediaFormat;
import com.socials.extractor.model.MediaResult;
import com.socials.extractor.model.Platform;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.ArrayList;
import java.util.List;

/**
 * Rewrites the raw CDN URLs in a {@link MediaResult} into proxy URLs served by
 * this backend, so clients download through {@link MediaProxyController} instead
 * of hitting the CDN directly (which fails for hot-link-protected platforms).
 *
 * <p>The rewrite is deliberately platform-agnostic: it wraps <em>every</em>
 * format URL. Meta URLs would work un-proxied, but routing them through the same
 * path keeps client behaviour uniform and shields against future tightening of
 * Meta's hot-link rules — one code path that works for all platforms.
 */
@Component
public class ProxyUrlRewriter {

    /** Path of the streaming endpoint exposed by {@link MediaProxyController}. */
    public static final String DOWNLOAD_PATH = "/api/v1/download";

    /**
     * @param result  the extraction result to rewrite (may be null)
     * @param baseUrl public base URL of this service, e.g. {@code http://192.168.100.12:8080}
     */
    public MediaResult rewrite(MediaResult result, String baseUrl) {
        if (result == null || !StringUtils.hasText(baseUrl)) {
            return result;
        }

        Platform platform = result.getPlatform();

        if (StringUtils.hasText(result.getUrl())) {
            result.setUrl(proxied(result.getUrl(), platform, baseUrl, filename(result, null)));
        }

        if (result.getFormats() != null) {
            List<MediaFormat> rewritten = new ArrayList<>(result.getFormats().size());
            int idx = 0;
            for (MediaFormat f : result.getFormats()) {
                if (f != null && StringUtils.hasText(f.getUrl())) {
                    f.setUrl(proxied(f.getUrl(), platform, baseUrl, filename(result, ++idx)));
                }
                rewritten.add(f);
            }
            result.setFormats(rewritten);
        }

        return result;
    }

    private String proxied(String cdnUrl, Platform platform, String baseUrl, String filename) {
        UriComponentsBuilder b = UriComponentsBuilder.fromUriString(baseUrl)
                .path(DOWNLOAD_PATH)
                .queryParam("u", MediaProxyService.encode(cdnUrl));

        if (platform != null && platform != Platform.UNKNOWN) {
            b.queryParam("p", platform.name());
        }
        if (StringUtils.hasText(filename)) {
            b.queryParam("n", filename);
        }
        // build(true) => the already-encoded 'u' value is not double-encoded.
        return b.build(true).toUriString();
    }

    private String filename(MediaResult result, Integer index) {
        String base = "video";
        if (result != null && StringUtils.hasText(result.getTitle())) {
            // Keep it URL-safe: no spaces (the URL is built with build(true),
            // which does not re-encode), letters/digits/-/_ only.
            base = result.getTitle()
                    .replaceAll("[^a-zA-Z0-9-_ ]", "")
                    .trim()
                    .replaceAll("\\s+", "_");
            if (base.length() > 60) {
                base = base.substring(0, 60);
            }
            if (base.isBlank()) {
                base = "video";
            }
        }
        return index == null ? base + ".mp4" : base + "-" + index + ".mp4";
    }
}
