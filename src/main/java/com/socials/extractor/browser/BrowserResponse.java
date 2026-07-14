package com.socials.extractor.browser;

import lombok.Builder;
import lombok.Data;

import java.nio.charset.StandardCharsets;
import java.util.Map;

@Data
@Builder
public class BrowserResponse {

    private String url;

    private String method;

    private String resourceType;

    private int status;

    private String contentType;

    private Map<String,String> headers;

    private byte[] body;

    public long contentLength() {

        try {

            return Long.parseLong(

                    headers.getOrDefault(
                            "content-length",
                            "0"
                    )

            );

        }

        catch (Exception ignored) {

            return 0L;

        }

    }

    public String bodyAsString() {

        if (body == null) {
            return "";
        }

        return new String(
                body,
                StandardCharsets.UTF_8
        );

    }

}