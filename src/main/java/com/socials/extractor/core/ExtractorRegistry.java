package com.socials.extractor.core;

import java.util.List;

public interface ExtractorRegistry {

    List<Extractor> getExtractors();

    default Extractor find(String url) {

        return getExtractors()

                .stream()

                .filter(extractor -> extractor.supports(url))

                .findFirst()

                .orElseThrow(() ->

                        new IllegalArgumentException(
                                "Unsupported platform: " + url
                        )

                );

    }

}