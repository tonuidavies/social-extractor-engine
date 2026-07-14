package com.socials.extractor.platforms.meta.script;

import com.socials.extractor.browser.BrowserCapture;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class DefaultMetaScriptExtractor
        implements MetaScriptExtractor {

    @Override
    public List<String> extract(BrowserCapture capture) {

        Document document = Jsoup.parse(capture.getHtml());

        List<String> scripts = new ArrayList<>();

        for (Element script : document.select("script")) {

            String text = script.data();

            if (text != null && !text.isBlank()) {
                scripts.add(text);
            }
        }

        return scripts;
    }
}