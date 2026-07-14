package com.socials.extractor.core.serverjs;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Component;

@Component
public class DefaultServerJsExtractor
        implements ServerJsExtractor {

    @Override
    public ServerJsPayload extract(
            Document document
    ) {

        ServerJsPayload payload =
                ServerJsPayload.builder()
                        .build();

        for (Element script : document.select("script")) {

            String html = script.html();

            if (html == null || html.isBlank()) {
                continue;
            }

            if (script.hasAttr("data-sjs")) {

                payload.getPayloads().add(html);

                continue;

            }

            if ("application/json".equals(script.attr("type"))) {

                payload.getPayloads().add(html);

            }

        }

        return payload;

    }

}