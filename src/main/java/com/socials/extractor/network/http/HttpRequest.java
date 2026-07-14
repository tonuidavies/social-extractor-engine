package com.socials.extractor.network.http;

import lombok.Builder;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;

@Data
@Builder
public class HttpRequest {

    private String url;

    @Builder.Default
    private Map<String, String> headers =
            new HashMap<>();

}