package com.socials.extractor.service;

import java.io.OutputStream;
import java.util.Map;

public interface ScraperService {
    // Extracts the metadata and hidden URL
    Map<String, String> getVideoInfo(String userUrl) throws Exception;

    // Streams the raw video bytes directly into memory
    void streamVideoBytes(String userUrl, OutputStream out) throws Exception;
}