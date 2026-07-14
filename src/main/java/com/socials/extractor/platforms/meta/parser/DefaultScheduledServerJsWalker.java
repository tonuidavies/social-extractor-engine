package com.socials.extractor.platforms.meta.parser;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class DefaultScheduledServerJsWalker
        implements ScheduledServerJsWalker {

    private final ObjectMapper mapper;

    @Override
    public Optional<JsonNode> walk(
            Document document
    ) {

        for (Element script : document.select("script")) {

            String javascript =
                    script.html();

            if (javascript == null || javascript.isBlank()) {
                continue;
            }

            if (!javascript.contains("__bbox")) {
                continue;
            }

            System.out.println();
            System.out.println("========== SERVER JS ==========");
            System.out.println(
                    javascript.substring(
                            0,
                            Math.min(
                                    1000,
                                    javascript.length()
                            )
                    )
            );
            System.out.println("===============================");

            /*
             * For now we only inspect.
             *
             * Next step:
             *
             * locate the JSON fragment
             * inside this JavaScript.
             */

            break;

        }

        return Optional.empty();

    }

}