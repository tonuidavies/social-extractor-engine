package com.socials.extractor.core.serverjs;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class DefaultServerJsDecoder
        implements ServerJsDecoder {

    private final ObjectMapper mapper;

    @Override
    public List<JsonNode> decode(
            ServerJsPayload payload
    ) {

        List<JsonNode> result =
                new ArrayList<>();

        for (String json : payload.getPayloads()) {

            try {

                result.add(

                        mapper.readTree(json)

                );

            }

            catch (Exception ignored) {

                /*
                 * Ignore non-JSON ServerJS payloads.
                 * Some blocks are JavaScript instead of JSON.
                 */

            }

        }

        return result;

    }

}