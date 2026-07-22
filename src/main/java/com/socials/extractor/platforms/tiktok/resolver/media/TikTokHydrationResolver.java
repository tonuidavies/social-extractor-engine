package com.socials.extractor.platforms.tiktok.resolver.media;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.socials.extractor.browser.BrowserCapture;
import com.socials.extractor.model.MediaFormat;
import com.socials.extractor.model.MediaResult;
import com.socials.extractor.model.Platform;
import com.socials.extractor.platforms.tiktok.resolver.browser.TikTokBrowserCaptureResolver;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Iterator;

/**
 * Reads TikTok's hydration JSON (__UNIVERSAL_DATA_FOR_REHYDRATION__ / SIGI_STATE)
 * and turns it into media formats.
 *
 * WATERMARK NOTE — this is the important bit:
 *   • video.playAddr    → the streaming URL used by the web/app player. CLEAN (no watermark).
 *   • video.downloadAddr → the URL TikTok uses for the in-app "Save video" action.
 *                          This file is WATERMARKED (TikTok logo + @username burned in).
 *
 * The previous version labelled downloadAddr as "No Watermark" and set it as the
 * primary URL, which is why saved TikToks came out watermarked. This version
 * prefers playAddr (clean) and only offers downloadAddr as a labelled fallback.
 */
@Component
@Order(1)
public class TikTokHydrationResolver implements TikTokBrowserCaptureResolver {

    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public boolean supports(BrowserCapture capture) {
        if (capture == null || capture.getHtml() == null) return false;
        return capture.getHtml().contains("__UNIVERSAL_DATA_FOR_REHYDRATION__")
                || capture.getHtml().contains("SIGI_STATE");
    }

    @Override
    public MediaResult resolve(BrowserCapture capture) {
        MediaResult result = MediaResult.builder()
                .platform(Platform.TIKTOK)
                .formats(new ArrayList<>())
                .build();

        try {
            Document document = Jsoup.parse(capture.getHtml());
            Element script = document.selectFirst("script#__UNIVERSAL_DATA_FOR_REHYDRATION__");
            if (script == null) {
                script = document.selectFirst("script#SIGI_STATE");
            }
            if (script != null) {
                JsonNode root = mapper.readTree(script.html());
                visit(root, result);
            }
        } catch (Exception ignored) {
        }

        return result;
    }

    private void visit(JsonNode node, MediaResult result) {
        if (node == null) return;

        if (node.has("playAddr") || node.has("downloadAddr")) {

            String playUrl = node.path("playAddr").asText(null);      // CLEAN — no watermark
            String downloadUrl = node.path("downloadAddr").asText(null); // WATERMARKED

            Integer width = node.has("width") ? node.get("width").asInt() : null;
            Integer height = node.has("height") ? node.get("height").asInt() : null;
            String q = (height != null && height >= 720) ? "HD" : "SD";

            // 1) Preferred: the clean, no-watermark play URL. Also becomes the primary URL.
            if (playUrl != null && !playUrl.isBlank()) {
                result.getFormats().add(
                        MediaFormat.builder()
                                .url(playUrl)
                                .mimeType("video/mp4")
                                .width(width)
                                .height(height)
                                .quality(q + " · No Watermark")
                                .build());
                if (result.getUrl() == null) result.setUrl(playUrl);
            }

            // 2) Fallback only: the watermarked download URL, clearly labelled.
            if (downloadUrl != null
                    && !downloadUrl.isBlank()
                    && !downloadUrl.equals(playUrl)) {
                result.getFormats().add(
                        MediaFormat.builder()
                                .url(downloadUrl)
                                .mimeType("video/mp4")
                                .width(width)
                                .height(height)
                                .quality(q + " · Watermarked")
                                .build());
                if (result.getUrl() == null) result.setUrl(downloadUrl);
            }
        }

        // Metadata
        if (result.getTitle() == null && node.has("desc")) {
            result.setTitle(node.get("desc").asText());
        }
        if (result.getThumbnail() == null) {
            if (node.has("originCover")) result.setThumbnail(node.get("originCover").asText());
            else if (node.has("cover")) result.setThumbnail(node.get("cover").asText());
        }
        if (result.getDuration() == null && node.has("duration")) {
            result.setDuration(node.get("duration").asLong());
        }

        // Recurse
        if (node.isObject()) {
            Iterator<JsonNode> it = node.elements();
            while (it.hasNext()) visit(it.next(), result);
        } else if (node.isArray()) {
            for (JsonNode child : node) visit(child, result);
        }
    }
}