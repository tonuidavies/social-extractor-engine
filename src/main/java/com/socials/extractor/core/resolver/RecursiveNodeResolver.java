package com.socials.extractor.core.resolver;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;

import java.util.Iterator;
import java.util.Optional;

@Component
public class RecursiveNodeResolver
        implements NodeResolver {

    @Override
    public Optional<JsonNode> resolve(
            JsonNode node,
            NodeMatcher matcher
    ) {

        if (matcher.matches(node)) {
            return Optional.of(node);
        }

        if (node.isObject()) {

            Iterator<JsonNode> iterator =
                    node.elements();

            while (iterator.hasNext()) {

                Optional<JsonNode> result =
                        resolve(
                                iterator.next(),
                                matcher
                        );

                if (result.isPresent()) {
                    return result;
                }

            }

        }

        if (node.isArray()) {

            for (JsonNode child : node) {

                Optional<JsonNode> result =
                        resolve(
                                child,
                                matcher
                        );

                if (result.isPresent()) {
                    return result;
                }

            }

        }

        return Optional.empty();

    }

}