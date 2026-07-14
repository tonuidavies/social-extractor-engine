package com.socials.extractor.platforms.meta.parser;

import com.fasterxml.jackson.databind.JsonNode;
import com.socials.extractor.platforms.meta.model.MetaMediaDocument;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DefaultMediaDocumentBuilder
        implements MediaDocumentBuilder {

    private final MetaMediaLocator locator;

    @Override
    public MetaMediaDocument build(
            JsonNode json
    ) {

        JsonNode media =

                locator.locate(json)

                        .orElseThrow();

        return MetaMediaDocument

                .builder()

                .root(json)

                .media(media)

                .mediaId(

                        text(
                                media,
                                "id"
                        )

                )

                .shortcode(

                        text(
                                media,
                                "shortcode"
                        )

                )

                .ownerId(

                        media.path("owner")

                                .path("id")

                                .asText()

                )

                .video(

                        media.has("video_url")

                )

                .carousel(

                        media.has("carousel_media")

                )

                .story(false)

                .reel(true)

                .build();

    }

    private String text(
            JsonNode node,
            String field
    ) {

        JsonNode value =
                node.get(field);

        return value == null
                ? null
                : value.asText();

    }

}