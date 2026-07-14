package com.socials.extractor.core.resolver;

import com.fasterxml.jackson.databind.JsonNode;

@FunctionalInterface
public interface NodeMatcher {

    boolean matches(
            JsonNode node
    );

}