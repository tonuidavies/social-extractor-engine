package com.socials.extractor.core.document;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Component;

@Component
public class DefaultDocumentAnalyzer
        implements DocumentAnalyzer {

    @Override
    public DocumentAnalysis analyze(
            Document document
    ) {

        DocumentAnalysis analysis =
                DocumentAnalysis.builder()
                        .build();

        for (Element script : document.select("script")) {

            String content =
                    script.data();

            if (content.isBlank()) {
                continue;
            }

            ArtifactType type =
                    script.attr("type")
                            .contains("json")
                            ? ArtifactType.JSON
                            : ArtifactType.JAVASCRIPT;

            analysis.getArtifacts()

                    .add(

                            DocumentArtifact.builder()

                                    .type(type)

                                    .content(content)

                                    .build()

                    );

        }

        return analysis;

    }

}