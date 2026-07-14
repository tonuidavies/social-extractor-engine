package com.socials.extractor.platforms.meta.parser;

import com.fasterxml.jackson.databind.JsonNode;
import org.jsoup.nodes.Document;

import java.util.Optional;

public interface ScheduledServerJsWalker {

    Optional<JsonNode> walk(
            Document document
    );

}