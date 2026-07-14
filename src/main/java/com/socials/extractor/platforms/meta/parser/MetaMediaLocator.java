package com.socials.extractor.platforms.meta.parser;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.Optional;

public interface MetaMediaLocator {

    Optional<JsonNode> locate(
            JsonNode document
    );

}