package com.socials.extractor.platforms.meta.matcher;

import com.fasterxml.jackson.databind.JsonNode;
import com.socials.extractor.core.resolver.NodeMatcher;
import org.springframework.stereotype.Component;

@Component
public class MetaMediaMatcher
        implements NodeMatcher {

    @Override
    public boolean matches(
            JsonNode node
    ) {

        return node.has("video_url")
                || node.has("carousel_media")
                || node.has("image_versions2")
                || node.has("video_versions")
                || node.has("display_url");

    }

}