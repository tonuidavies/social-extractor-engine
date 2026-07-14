package com.socials.extractor.core.html;

import org.jsoup.nodes.Document;

import java.util.List;

public interface HtmlScriptExtractor {

    List<String> extract(
            Document document
    );

}