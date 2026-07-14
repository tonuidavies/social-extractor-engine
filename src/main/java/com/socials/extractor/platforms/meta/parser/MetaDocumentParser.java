package com.socials.extractor.platforms.meta.parser;

import com.socials.extractor.platforms.meta.model.MetaMediaDocument;
import org.jsoup.nodes.Document;

public interface MetaDocumentParser {

    MetaMediaDocument parse(
            Document document
    ) throws Exception;

}