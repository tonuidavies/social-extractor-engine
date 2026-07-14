package com.socials.extractor.core.serverjs;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;

public interface ServerJsParser {

    List<JsonNode> parse(
            ServerJsPayload payload
    ) throws Exception;

}