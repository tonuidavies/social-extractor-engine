package com.socials.extractor.core.html;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class JsoupHtmlScriptExtractor
        implements HtmlScriptExtractor {

    @Override
    public List<String> extract(
            Document document
    ) {

        List<String> scripts =
                new ArrayList<>();

        for (Element script : document.select("script")) {

            String html =
                    script.html();

            if (!html.isBlank()) {

                scripts.add(html);

            }

        }

        return scripts;

    }

}