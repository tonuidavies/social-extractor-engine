package com.socials.extractor.core.json.tree;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;

import java.util.Iterator;
import java.util.Optional;
import java.util.function.Predicate;

@Component
public class DefaultJsonTreeWalker
        implements JsonTreeWalker {

    @Override
    public Optional<JsonNode> find(
            JsonNode node,
            Predicate<JsonNode> predicate
    ) {

        if (predicate.test(node)) {
            return Optional.of(node);
        }

        if (node.isObject()) {

            Iterator<JsonNode> iterator =
                    node.elements();

            while (iterator.hasNext()) {

                Optional<JsonNode> result =
                        find(
                                iterator.next(),
                                predicate
                        );

                if (result.isPresent()) {
                    return result;
                }

            }

        }

        if (node.isArray()) {

            for (JsonNode child : node) {

                Optional<JsonNode> result =
                        find(
                                child,
                                predicate
                        );

                if (result.isPresent()) {
                    return result;
                }

            }

        }

        return Optional.empty();

    }

}