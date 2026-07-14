package com.socials.extractor.browser.debug;

import com.socials.extractor.browser.BrowserCapture;
import com.socials.extractor.browser.BrowserResponse;

import java.util.Comparator;

public class BrowserCaptureInspector {

    public void inspect(
            BrowserCapture capture
    ) {

        System.out.println();
        System.out.println("============= RESPONSES =============");

        capture.getResponses()

                .stream()

                .sorted(
                        Comparator.comparing(
                                BrowserResponse::getContentType,
                                Comparator.nullsLast(String::compareTo)
                        )
                )

                .forEach(this::print);

    }

    private void print(
            BrowserResponse response
    ) {

        int size =

                response.getBody() == null

                        ? 0

                        : response.getBody().length;

        System.out.printf(

                "%3d | %-35s | %8d | %s%n",

                response.getStatus(),

                response.getContentType(),

                size,

                response.getUrl()

        );

    }

}