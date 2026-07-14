package com.socials.extractor.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BrowserSession {

    private String userAgent;

    private String acceptLanguage;

    private String acceptEncoding;

    private String referer;

    @Builder.Default
    private Map<String, String> headers =
            new HashMap<>();

    @Builder.Default
    private Map<String, String> cookies =
            new HashMap<>();

}