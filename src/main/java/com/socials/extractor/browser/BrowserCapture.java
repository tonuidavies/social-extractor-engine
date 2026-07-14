package com.socials.extractor.browser;

import com.socials.extractor.browser.event.BrowserEvent;
import lombok.Builder;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
public class BrowserCapture {

    private String finalUrl;

    private String html;

    @Builder.Default
    private List<BrowserRequest> requests =
            new ArrayList<>();

    @Builder.Default
    private List<BrowserResponse> responses =
            new ArrayList<>();

    public List<BrowserResponse> jsonResponses() {

        return responses

                .stream()

                .filter(r ->

                        r.getContentType() != null

                                && r.getContentType()

                                .contains("json")

                )

                .toList();

    }


    public List<BrowserResponse> videoResponses() {

        return responses

                .stream()

                .filter(r -> {

                    String type =

                            r.getContentType();

                    return type != null &&

                            (type.contains("video")

                                    || type.contains("mp4")

                                    || type.contains("mpegurl")

                                    || type.contains("dash"));

                })

                .toList();

    }

}