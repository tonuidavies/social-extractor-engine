package com.socials.extractor.platforms.meta.resolver.media;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.socials.extractor.browser.BrowserCapture;
import com.socials.extractor.browser.BrowserResponse;
import com.socials.extractor.model.MediaFormat;
import com.socials.extractor.model.MediaResult;
import com.socials.extractor.model.Platform;
import com.socials.extractor.platforms.meta.resolver.browser.BrowserCaptureResolver;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Iterator;

@Component
@Order(1)
public class MetaGraphqlResolver
        implements BrowserCaptureResolver {

    private final ObjectMapper mapper =
            new ObjectMapper();

    @Override
    public boolean supports(
            BrowserCapture capture
    ) {

        return capture.getResponses()

                .stream()

                .anyMatch(r ->

                        r.getContentType() != null

                                &&

                                (
                                        r.getContentType().contains("json")
                                                || r.getUrl().contains("graphql")
                                )

                );

    }

    @Override
    public MediaResult resolve(
            BrowserCapture capture
    ) {

        MediaResult result =
                MediaResult.builder()

                        .platform(
                                Platform.INSTAGRAM
                        )

                        .formats(new ArrayList<>())

                        .build();

        for (BrowserResponse response : capture.getResponses()) {

            if (response.getBody() == null) {
                continue;
            }

            try {

                JsonNode root =
                        mapper.readTree(response.getBody());

                visit(root, result);

            }

            catch (Exception ignored) {

            }

        }

        return result;

    }

    private void visit(
            JsonNode node,
            MediaResult result
    ) {

        if (node == null) {
            return;
        }

        /*
         * video_versions
         */
        if (node.has("video_versions")) {

            JsonNode versions =
                    node.get("video_versions");

            if (versions.isArray()) {

                for (JsonNode version : versions) {

                    String url =
                            version.path("url").asText(null);

                    if (url == null) {
                        continue;
                    }

                    result.getFormats().add(

                            MediaFormat.builder()

                                    .url(url)

                                    .mimeType("video/mp4")

                                    .width(
                                            version.has("width")
                                                    ? version.get("width").asInt()
                                                    : null
                                    )

                                    .height(
                                            version.has("height")
                                                    ? version.get("height").asInt()
                                                    : null
                                    )

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

            String thumb =
                    findImage(node);

            if (thumb != null) {

                result.setThumbnail(
                        thumb
                );

            }

        }

        /*
         * title
         */
        if (result.getTitle() == null) {

            String title =
                    findCaption(node);

            if (title != null) {

                result.setTitle(
                        title
                );

            }

        }

        /*
         * duration
         */
        if (result.getDuration() == null
                && node.has("video_duration")) {

            result.setDuration(

                    (long)

                            node.get("video_duration")

                                    .asDouble()

            );

        }

        if (node.isObject()) {

            Iterator<JsonNode> iterator =
                    node.elements();

            while (iterator.hasNext()) {

                visit(
                        iterator.next(),
                        result
                );

            }

        }

        if (node.isArray()) {

            for (JsonNode child : node) {

                visit(
                        child,
                        result
                );

            }

        }

    }

    private String findCaption(
            JsonNode node
    ) {

        if (!node.has("caption")) {
            return null;
        }

        JsonNode caption =
                node.get("caption");

        if (caption.isTextual()) {

            return caption.asText();

        }

        if (caption.has("text")) {

            return caption.get("text").asText();

        }

        return null;

    }

    private String findImage(
            JsonNode node
    ) {

        if (!node.has("image_versions2")) {
            return null;
        }

        JsonNode candidates =

                node.path("image_versions2")

                        .path("candidates");

        if (!candidates.isArray()
                || candidates.isEmpty()) {

            return null;

        }

        return candidates

                .get(0)

                .path("url")

                .asText(null);

    }

}