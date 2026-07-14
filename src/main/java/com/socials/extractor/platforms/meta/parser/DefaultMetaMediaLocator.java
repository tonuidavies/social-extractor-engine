package com.socials.extractor.platforms.meta.parser;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;

import java.util.Iterator;
import java.util.Map;
import java.util.Optional;

@Component
public class DefaultMetaMediaLocator
        implements MetaMediaLocator {

    @Override
    public Optional<JsonNode> locate(
            JsonNode root
    ) {

        return find(root);

    }

    private Optional<JsonNode> find(
            JsonNode node
    ) {

        if (node == null) {
            return Optional.empty();
        }

        if (looksLikeMedia(node)) {
            return Optional.of(node);
        }

        if (node.isObject()) {

            Iterator<Map.Entry<String, JsonNode>> fields =
                    node.fields();

            while (fields.hasNext()) {

                Map.Entry<String, JsonNode> entry =
                        fields.next();

                Optional<JsonNode> result =
                        find(entry.getValue());

                if (result.isPresent()) {
                    return result;
                }

            }

        }

        if (node.isArray()) {

            for (JsonNode child : node) {

                Optional<JsonNode> result =
                        find(child);

                if (result.isPresent()) {
                    return result;
                }

            }

        }

        return Optional.empty();

    }

    private boolean looksLikeMedia(
            JsonNode node
    ) {

        return node.has("video_url")
                || node.has("image_versions2")
                || node.has("display_url")
                || node.has("carousel_media")
                || node.has("video_versions");

    }

}