package com.socials.extractor.core.parser;

import org.jsoup.nodes.Document;

public interface HtmlParser {

    Document parse(
            String html
    );

}