package com.socials.extractor.model;

import lombok.Builder;
import lombok.Data;

import java.net.http.HttpHeaders;
import java.util.Map;
@Data
@Builder
public class BrowserResponse {

    private int status;

    private String body;

    private String finalUrl;

    private HttpHeaders headers;

}