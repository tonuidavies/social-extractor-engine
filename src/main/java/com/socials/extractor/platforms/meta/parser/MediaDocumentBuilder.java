package com.socials.extractor.platforms.meta.parser;

import com.fasterxml.jackson.databind.JsonNode;
import com.socials.extractor.platforms.meta.model.MetaMediaDocument;

public interface MediaDocumentBuilder {

    MetaMediaDocument build(
            JsonNode json
    );

}