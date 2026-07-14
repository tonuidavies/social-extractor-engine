package com.socials.extractor.browser.playwright.capture;

import com.microsoft.playwright.Page;
import com.socials.extractor.browser.BrowserRequest;
import com.socials.extractor.browser.playwright.model.CaptureState;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class RequestCollector {

    public void register(
            Page page,
            CaptureState state
    ) {

        page.onRequest(request -> {

            BrowserRequest browserRequest =

                    BrowserRequest.builder()

                            .url(request.url())

                            .method(request.method())

                            .resourceType(request.resourceType())

                            .headers(request.headers())

                            .postData(request.postData())

                            .build();

            state.getRequests().add(browserRequest);

            /*
             * Temporary debugging.
             * We want to verify whether Instagram is issuing GraphQL
             * requests after the page loads.
             */
            if (request.url().contains("/api/graphql")) {

                log.info(
                        "GRAPHQL REQUEST -> {}",
                        request.url()
                );

            }

        });

    }

}