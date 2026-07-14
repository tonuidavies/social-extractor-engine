package com.socials.extractor.browser;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
public class BrowserRequest {

    private String url;

    private String method;

    private String resourceType;

    private Map<String, String> headers;

    private String postData;

}