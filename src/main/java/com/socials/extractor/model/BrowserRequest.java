package com.socials.extractor.model;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Builder
@Data
public class BrowserRequest {

    private String url;

    private String method;

    private BrowserSession session;

    private Map<String,String> headers;

    private String body;

}