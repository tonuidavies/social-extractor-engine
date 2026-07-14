package com.socials.extractor.core.serverjs;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class DefaultServerJsParser
        implements ServerJsParser {

    private final ObjectMapper mapper;

    @Override
    public List<JsonNode> parse(
            ServerJsPayload payload
    ) throws Exception {

        List<JsonNode> result =
                new ArrayList<>();

        for (String json : payload.getPayloads()) {

            try {

                result.add(
                        mapper.readTree(json)
                );

            }
            catch (Exception ignored) {
            }

        }

        return result;

    }

}