package com.socials.extractor.platforms.meta.resolver.media;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.socials.extractor.browser.BrowserCapture;
import com.socials.extractor.browser.BrowserResponse;
import com.socials.extractor.model.MediaFormat;
import com.socials.extractor.model.MediaResult;
import com.socials.extractor.model.Platform;
import com.socials.extractor.platforms.meta.resolver.browser.BrowserCaptureResolver;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@Order(1)
public class MetaGraphqlResolver implements BrowserCaptureResolver {

    private final ObjectMapper mapper = new ObjectMapper();
    private static final Pattern EFG_PATTERN = Pattern.compile("[?&]efg=([^&]+)");

    @Override
    public boolean supports(BrowserCapture capture) {
        boolean hasJsonResponses = capture.getResponses().stream()
                .anyMatch(r -> r.getContentType() != null &&
                        (r.getContentType().contains("json") || r.getUrl().contains("graphql")));
        boolean hasHtml = capture.getHtml() != null && !capture.getHtml().isBlank();
        return hasJsonResponses || hasHtml;
    }

    private Platform resolvePlatform(BrowserCapture capture) {
        String url = capture.getFinalUrl();
        if (url == null) return Platform.INSTAGRAM;
        url = url.toLowerCase();
        if (url.contains("facebook.com") || url.contains("fb.watch")) return Platform.FACEBOOK;
        return Platform.INSTAGRAM;
    }

    @Override
    public MediaResult resolve(BrowserCapture capture) {
        MediaResult result = MediaResult.builder()
                .platform(resolvePlatform(capture))
                .formats(new ArrayList<>())
                .build();

        for (BrowserResponse response : capture.getResponses()) {
            if (response.getBody() == null) continue;
            try {
                JsonNode root = mapper.readTree(response.getBody());
                visit(root, result);
            } catch (Exception ignored) {}
        }

        if (capture.getHtml() != null && !capture.getHtml().isBlank()) {
            try {
                Document document = Jsoup.parse(capture.getHtml());
                for (Element script : document.select("script")) {
                    String text = script.html();
                    if (text == null || text.isBlank()) continue;
                    if ("application/json".equals(script.attr("type")) || script.hasAttr("data-sjs") || text.contains("__bbox")) {
                        try {
                            JsonNode root = mapper.readTree(text);
                            visit(root, result);
                        } catch (Exception ignored) {}
                    }
                }
            } catch (Exception ignored) {}
        }

        return result;
    }

    private void visit(JsonNode node, MediaResult result) {
        if (node == null) return;

        /*
         * video_url with Width, Height, and Quality
         */
        if (node.has("video_url")) {
            String url = node.get("video_url").asText(null);
            if (url != null && !url.isBlank()) {

                Integer width = null;
                Integer height = null;

                if (node.has("original_width")) width = node.get("original_width").asInt();
                if (node.has("original_height")) height = node.get("original_height").asInt();

                if (width == null && node.has("dimensions")) {
                    JsonNode dims = node.get("dimensions");
                    if (dims.has("width")) width = dims.get("width").asInt();
                    if (dims.has("height")) height = dims.get("height").asInt();
                }

                addFormatIfNotExists(result, url, width, height);
            }
        }

        /*
         * video_versions
         */
        if (node.has("video_versions")) {
            JsonNode versions = node.get("video_versions");
            if (versions.isArray()) {
                for (JsonNode version : versions) {
                    String url = version.path("url").asText(null);
                    if (url == null) continue;

                    Integer width = version.has("width") ? version.get("width").asInt() : null;
                    Integer height = version.has("height") ? version.get("height").asInt() : null;

                    addFormatIfNotExists(result, url, width, height);
                }
            }
        }

        /* Metadata Extraction */
        if (result.getThumbnail() == null) {
            String thumb = findImage(node);
            if (thumb != null) result.setThumbnail(thumb);
        }

        if (result.getTitle() == null) {
            String title = findCaption(node);
            if (title != null) result.setTitle(title);
        }

        if (result.getDuration() == null) {
            if (node.has("video_duration")) {
                result.setDuration((long) node.get("video_duration").asDouble());
            } else if (node.has("playable_duration_in_ms")) {
                result.setDuration(node.get("playable_duration_in_ms").asLong() / 1000);
            }
        }

        /* Recurse through JSON */
        if (node.isObject()) {
            Iterator<JsonNode> iterator = node.elements();
            while (iterator.hasNext()) visit(iterator.next(), result);
        }
        if (node.isArray()) {
            for (JsonNode child : node) visit(child, result);
        }
    }

    /**
     * Prevents duplicate URLs from cluttering the formats array and automatically
     * extracts missing metadata from the URL string.
     */
    private void addFormatIfNotExists(MediaResult result, String url, Integer width, Integer height) {
        // Prevent exact duplicate URLs
        boolean exists = result.getFormats().stream().anyMatch(f -> url.equals(f.getUrl()));
        if (exists) return;

        result.getFormats().add(
                MediaFormat.builder()
                        .url(url)
                        .mimeType("video/mp4")
                        .width(width)
                        .height(height)
                        .quality(determineQuality(url, height))
                        .build()
        );

        if (result.getUrl() == null) {
            result.setUrl(url);
        }

        // Check the EFG payload for duration if we haven't found it yet
        if (result.getDuration() == null) {
            JsonNode efgNode = decodeEfg(url);
            if (efgNode != null && efgNode.has("duration_s")) {
                result.setDuration(efgNode.get("duration_s").asLong());
            }
        }
    }

    /**
     * Helper method to translate resolution heights or URL tags into quality strings.
     */
    private String determineQuality(String url, Integer height) {
        if (height != null) {
            if (height >= 1080) return "1080p";
            if (height >= 720) return "720p";
            if (height >= 480) return "480p";
            if (height >= 360) return "360p";
            return "SD";
        }

        if (url != null) {
            String lowerUrl = url.toLowerCase();
            if (lowerUrl.contains("1080p")) return "1080p";
            if (lowerUrl.contains("720p")) return "720p";
            if (lowerUrl.contains("480p")) return "480p";
            if (lowerUrl.contains("360p")) return "360p";

            // If it's an Instagram link, decode the EFG parameter to find the quality
            JsonNode efgNode = decodeEfg(url);
            if (efgNode != null && efgNode.has("vencode_tag")) {
                String vencode = efgNode.get("vencode_tag").asText().toLowerCase();
                if (vencode.contains("1080")) return "1080p";
                if (vencode.contains("720")) return "720p";
                if (vencode.contains("480")) return "480p";
                if (vencode.contains("360")) return "360p";
            }
        }

        return null;
    }

    /**
     * Decodes the Base64 JSON payload hidden inside Meta's "efg" URL parameter.
     */
    private JsonNode decodeEfg(String url) {
        if (url == null) return null;
        try {
            Matcher m = EFG_PATTERN.matcher(url);
            if (m.find()) {
                String efg = URLDecoder.decode(m.group(1), StandardCharsets.UTF_8);

                // Fix Base64 padding if Meta truncated it
                while (efg.length() % 4 != 0) {
                    efg += "=";
                }

                byte[] decoded;
                try {
                    decoded = Base64.getUrlDecoder().decode(efg);
                } catch (IllegalArgumentException e) {
                    decoded = Base64.getDecoder().decode(efg);
                }

                return mapper.readTree(new String(decoded, StandardCharsets.UTF_8));
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private String findCaption(JsonNode node) {
        if (!node.has("caption")) return null;
        JsonNode caption = node.get("caption");
        if (caption == null || caption.isNull()) return null;
        if (caption.isTextual()) return caption.asText();
        if (caption.has("text")) return caption.get("text").asText();
        return null;
    }

    private String findImage(JsonNode node) {
        if (!node.has("image_versions2")) return null;
        JsonNode candidates = node.path("image_versions2").path("candidates");
        if (!candidates.isArray() || candidates.isEmpty()) return null;
        return candidates.get(0).path("url").asText(null);
    }
}