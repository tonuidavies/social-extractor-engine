package com.socials.extractor.network.transport;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
public class BrowserResponse {

    private int status;

    private String body;

    private String finalUrl;

    private Map<String, String> headers;

}