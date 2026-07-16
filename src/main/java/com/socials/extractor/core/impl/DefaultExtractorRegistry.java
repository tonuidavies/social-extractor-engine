package com.socials.extractor.core.impl;

import com.socials.extractor.core.Extractor;
import com.socials.extractor.core.ExtractorRegistry;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DefaultExtractorRegistry implements ExtractorRegistry {
    private final List<Extractor> extractors;
    public DefaultExtractorRegistry(List<Extractor> extractors) {
        this.extractors = extractors;
    }

    @Override
    public List<Extractor> getExtractors() {
        return extractors;
    }

}