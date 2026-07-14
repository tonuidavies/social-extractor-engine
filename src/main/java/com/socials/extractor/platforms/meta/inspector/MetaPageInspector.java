package com.socials.extractor.platforms.meta.inspector;

import org.jsoup.nodes.Document;

public interface MetaPageInspector {

    InspectionReport inspect(
            Document document
    );

}