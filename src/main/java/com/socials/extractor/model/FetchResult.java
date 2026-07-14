package com.socials.extractor.model;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
public class FetchResult {

    private int status;

    private String body;

    private String finalUrl;

    private Map<String, String> headers;

}