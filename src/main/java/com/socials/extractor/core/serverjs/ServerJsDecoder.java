package com.socials.extractor.core.serverjs;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;

public interface ServerJsDecoder {

    List<JsonNode> decode(
            ServerJsPayload payload
    ) throws Exception;

}