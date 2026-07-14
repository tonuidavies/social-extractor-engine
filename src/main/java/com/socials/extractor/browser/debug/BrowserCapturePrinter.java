package com.socials.extractor.browser.debug;

import com.socials.extractor.browser.BrowserCapture;
import com.socials.extractor.browser.BrowserResponse;

public class BrowserCapturePrinter {

    public void print(BrowserCapture capture) {

        System.out.println();
        System.out.println("========== RESPONSES ==========");

        for (BrowserResponse response : capture.getResponses()) {

            System.out.printf(
                    "%3d | %-20s | %s%n",
                    response.getStatus(),
                    response.getContentType(),
                    response.getUrl()
            );

        }

    }

}