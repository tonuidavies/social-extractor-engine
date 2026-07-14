package com.socials.extractor.core.serverjs;

import org.jsoup.nodes.Document;

public interface ServerJsExtractor {

    ServerJsPayload extract(
            Document document
    );

}