package com.socials.extractor.core.json.tree;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.Optional;
import java.util.function.Predicate;

public interface JsonTreeWalker {

    Optional<JsonNode> find(
            JsonNode root,
            Predicate<JsonNode> predicate
    );

}