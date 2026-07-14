package com.socials.extractor.browser.playwright.capture;

import com.microsoft.playwright.Page;
import com.socials.extractor.browser.BrowserCapture;
import com.socials.extractor.browser.playwright.model.CaptureState;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;

@Slf4j
@Component
public class CaptureBuilder {

    public BrowserCapture build(
            Page page,
            CaptureState state
    ) {

        String html = "";

        try {

            html = page.content();

            Files.writeString(

                    Path.of("/tmp/capture.html"),

                    html

            );

            log.info("Saved capture HTML to /tmp/capture.html");

        }

        catch (Exception ex) {

            log.warn(
                    "Unable to capture page HTML because the page is navigating."
            );

        }

        return BrowserCapture.builder()

                .finalUrl(
                        page.url()
                )

                .html(
                        html
                )

                .requests(
                        state.getRequests()
                )

                .responses(
                        state.getResponses()
                )

                .build();

    }

}