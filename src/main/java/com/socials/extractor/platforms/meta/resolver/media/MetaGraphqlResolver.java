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

import java.util.ArrayList;
import java.util.Iterator;

@Component
@Order(1)
public class MetaGraphqlResolver implements BrowserCaptureResolver {

    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public boolean supports(BrowserCapture capture) {

        boolean hasJsonResponses = capture.getResponses().stream()
                .anyMatch(r -> r.getContentType() != null &&
                        (r.getContentType().contains("json") || r.getUrl().contains("graphql")));

        boolean hasHtml = capture.getHtml() != null && !capture.getHtml().isBlank();

        return hasJsonResponses || hasHtml;
    }

    @Override
    public MediaResult resolve(BrowserCapture capture) {

        MediaResult result = MediaResult.builder()
                .platform(Platform.INSTAGRAM)
                .formats(new ArrayList<>())
                .build();

        // 1. Process XHR Network Responses
        for (BrowserResponse response : capture.getResponses()) {
            if (response.getBody() == null) {
                continue;
            }
            try {
                JsonNode root = mapper.readTree(response.getBody());
                visit(root, result);
            } catch (Exception ignored) {
            }
        }

        // 2. Process Embedded JSON inside the HTML Document
        if (capture.getHtml() != null && !capture.getHtml().isBlank()) {
            try {
                Document document = Jsoup.parse(capture.getHtml());
                for (Element script : document.select("script")) {
                    String text = script.html();
                    if (text == null || text.isBlank()) {
                        continue;
                    }

                    // Look for JSON structures injected into scripts
                    if ("application/json".equals(script.attr("type")) || script.hasAttr("data-sjs") || text.contains("__bbox")) {
                        try {
                            JsonNode root = mapper.readTree(text);
                            visit(root, result);
                        } catch (Exception ignored) {
                            // Safely ignore if the script block isn't cleanly parsable JSON
                        }
                    }
                }
            } catch (Exception ignored) {
            }
        }

        return result;
    }

    private void visit(JsonNode node, MediaResult result) {
        if (node == null) {
            return;
        }

        /*
         * video_url (Standard posts & some Reels)
         */
        if (node.has("video_url")) {
            String url = node.get("video_url").asText(null);
            if (url != null && !url.isBlank()) {
                result.getFormats().add(
                        MediaFormat.builder()
                                .url(url)
                                .mimeType("video/mp4")
                                .build()
                );

                if (result.getUrl() == null) {
                    result.setUrl(url);
                }
            }
        }

        /*
         * video_versions (Alternate structures)
         */
        if (node.has("video_versions")) {
            JsonNode versions = node.get("video_versions");
            if (versions.isArray()) {
                for (JsonNode version : versions) {
                    String url = version.path("url").asText(null);
                    if (url == null) {
                        continue;
                    }

                    result.getFormats().add(
                            MediaFormat.builder()
                                    .url(url)
                                    .mimeType("video/mp4")
                                    .width(version.has("width") ? version.get("width").asInt() : null)
                                    .height(version.has("height") ? version.get("height").asInt() : null)
                                    .build()
                    );

                    if (result.getUrl() == null) {
                        result.setUrl(url);
                    }
                }
            }
        }

        /*
         * thumbnail
         */
        if (result.getThumbnail() == null) {
            String thumb = findImage(node);
            if (thumb != null) {
                result.setThumbnail(thumb);
            }
        }

        /*
         * title
         */
        if (result.getTitle() == null) {
            String title = findCaption(node);
            if (title != null) {
                result.setTitle(title);
            }
        }

        /*
         * duration
         */
        if (result.getDuration() == null && node.has("video_duration")) {
            result.setDuration((long) node.get("video_duration").asDouble());
        }

        // Recursively search child nodes
        if (node.isObject()) {
            Iterator<JsonNode> iterator = node.elements();
            while (iterator.hasNext()) {
                visit(iterator.next(), result);
            }
        }

        if (node.isArray()) {
            for (JsonNode child : node) {
                visit(child, result);
            }
        }
    }

    private String findCaption(JsonNode node) {
        if (!node.has("caption")) {
            return null;
        }
        JsonNode caption = node.get("caption");

        if (caption == null || caption.isNull()) {
            return null;
        }

        if (caption.isTextual()) {
            return caption.asText();
        }

        if (caption.has("text")) {
            return caption.get("text").asText();
        }
        return null;
    }

    private String findImage(JsonNode node) {
        if (!node.has("image_versions2")) {
            return null;
        }

        JsonNode candidates = node.path("image_versions2").path("candidates");

        if (!candidates.isArray() || candidates.isEmpty()) {
            return null;
        }

        return candidates.get(0).path("url").asText(null);
    }
}