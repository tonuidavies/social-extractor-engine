package com.socials.extractor.platforms.meta.inspector;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class DefaultMetaPageInspector
        implements MetaPageInspector {

    private static final Pattern URL_PATTERN =
            Pattern.compile("https?://[^\"'\\s]+");

    @Override
    public InspectionReport inspect(
            Document document
    ) {

        InspectionReport report =
                InspectionReport.builder()
                        .build();

        for (Element script : document.select("script")) {

            String data = script.data();

            if (data.isBlank()) {
                continue;
            }

            report.getScripts().add(data);

            if (script.attr("type").contains("json")) {

                report.getJsonScripts().add(data);

            }

            Matcher matcher =
                    URL_PATTERN.matcher(data);

            while (matcher.find()) {

                String url =
                        matcher.group();

                report.getUrls().add(url);

                if (url.contains("graphql")) {

                    report.getGraphqlEndpoints().add(url);

                }

            }

        }

        return report;

    }

}