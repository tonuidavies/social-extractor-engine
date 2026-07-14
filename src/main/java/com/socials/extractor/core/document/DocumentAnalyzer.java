package com.socials.extractor.core.document;

import org.jsoup.nodes.Document;

public interface DocumentAnalyzer {

    DocumentAnalysis analyze(
            Document document
    );

}