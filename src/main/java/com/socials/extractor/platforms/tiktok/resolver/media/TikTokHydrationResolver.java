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

@Component
@Order(1)
public class TikTokHydrationResolver implements TikTokBrowserCaptureResolver {

    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public boolean supports(BrowserCapture capture) {
        if (capture == null || capture.getHtml() == null) return false;
        return capture.getHtml().contains("__UNIVERSAL_DATA_FOR_REHYDRATION__") || capture.getHtml().contains("SIGI_STATE");
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
        } catch (Exception ignored) {}

        return result;
    }

    private void visit(JsonNode node, MediaResult result) {
        if (node == null) return;

        // Extract Video Formats
        if (node.has("playAddr") || node.has("downloadAddr")) {
            String playUrl = node.path("playAddr").asText(null);
            String downloadUrl = node.path("downloadAddr").asText(null); // Usually No-Watermark
            
            Integer width = node.has("width") ? node.get("width").asInt() : null;
            Integer height = node.has("height") ? node.get("height").asInt() : null;
            String quality = (height != null && height >= 720) ? "HD" : "SD";

            if (downloadUrl != null && !downloadUrl.isBlank()) {
                result.getFormats().add(MediaFormat.builder()
                        .url(downloadUrl)
                        .mimeType("video/mp4")
                        .width(width)
                        .height(height)
                        .quality(quality + " (No Watermark)")
                        .build());
                if (result.getUrl() == null) result.setUrl(downloadUrl);
            }

            if (playUrl != null && !playUrl.isBlank()) {
                result.getFormats().add(MediaFormat.builder()
                        .url(playUrl)
                        .mimeType("video/mp4")
                        .width(width)
                        .height(height)
                        .quality(quality)
                        .build());
                if (result.getUrl() == null) result.setUrl(playUrl);
            }
        }

        // Extract Metadata
        if (result.getTitle() == null && node.has("desc")) result.setTitle(node.get("desc").asText());
        if (result.getThumbnail() == null) {
            if (node.has("originCover")) result.setThumbnail(node.get("originCover").asText());
            else if (node.has("cover")) result.setThumbnail(node.get("cover").asText());
        }
        if (result.getDuration() == null && node.has("duration")) result.setDuration(node.get("duration").asLong());

        // Recursive tree walk
        if (node.isObject()) {
            Iterator<JsonNode> iterator = node.elements();
            while (iterator.hasNext()) visit(iterator.next(), result);
        } else if (node.isArray()) {
            for (JsonNode child : node) visit(child, result);
        }
    }
}