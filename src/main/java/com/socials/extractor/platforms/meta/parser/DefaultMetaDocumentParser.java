package com.socials.extractor.platforms.meta.parser;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.socials.extractor.platforms.meta.model.MetaMediaDocument;
import lombok.RequiredArgsConstructor;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DefaultMetaDocumentParser
        implements MetaDocumentParser {

    private final ObjectMapper mapper;

    private final MetaMediaLocator locator;

    private final MediaDocumentBuilder builder;

    @Override
    public MetaMediaDocument parse(
            Document document
    ) throws Exception {

        for (Element script : document.select("script")) {

            String html = script.html();

            if (html == null || html.isBlank()) {
                continue;
            }

            if (!html.contains("__bbox")) {
                continue;
            }

            try {

                JsonNode node =
                        mapper.readTree(html);

                if (locator.locate(node).isPresent()) {

                    return builder.build(node);

                }

            }

            catch (Exception ignored) {
            }

        }

        throw new IllegalStateException(
                "Instagram media not found."
        );

    }

}