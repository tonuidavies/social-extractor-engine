package com.socials.extractor.core.parser;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Component;

@Component
public class JsoupHtmlParser implements HtmlParser {

    @Override
    public Document parse(String html) {

        return Jsoup.parse(html);

    }

}