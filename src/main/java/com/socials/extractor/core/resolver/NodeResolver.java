package com.socials.extractor.core.resolver;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.Optional;

public interface NodeResolver {

    Optional<JsonNode> resolve(
            JsonNode root,
            NodeMatcher matcher
    );

}