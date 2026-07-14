package com.socials.extractor.browser.playwright.capture;

import com.microsoft.playwright.Page;
import com.socials.extractor.browser.BrowserResponse;
import com.socials.extractor.browser.playwright.model.CaptureState;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;

@Slf4j
@Component
public class ResponseCollector {

    private static final Path GRAPHQL_DUMP =
            Path.of("/tmp/graphql.json");

    public void register(
            Page page,
            CaptureState state
    ) {

        page.onResponse(response -> {

            try {

                String url =
                        response.url();

                String resourceType =
                        response.request().resourceType();

                String contentType =
                        response.headers()
                                .getOrDefault(
                                        "content-type",
                                        ""
                                );

                /*
                 * Log the interesting responses.
                 */
                if ("xhr".equals(resourceType)
                        || "fetch".equals(resourceType)
                        || url.contains("graphql")
                        || url.contains("/api/")
                        || url.contains("video")
                        || contentType.startsWith("video/")) {

                    log.info(
                            "RESPONSE [{}] {} {}",
                            resourceType,
                            contentType,
                            url
                    );

                }

                byte[] body = null;

                /*
                 * Only download textual payloads.
                 */
                if (contentType.contains("json")
                        || contentType.contains("javascript")
                        || contentType.contains("application/json")
                        || contentType.contains("text/html")
                        || contentType.startsWith("text/")) {

                    try {

                        body = response.body();

                    }

                    catch (Exception ex) {

                        log.debug(
                                "Unable to read body: {}",
                                url
                        );

                    }

                }

                /*
                 * Dump every GraphQL response.
                 */
                if (body != null
                        && url.toLowerCase().contains("graphql")) {

                    try {

                        Files.write(
                                GRAPHQL_DUMP,
                                body
                        );

                        log.info(
                                "Saved GraphQL response -> {}",
                                GRAPHQL_DUMP
                        );

                    }

                    catch (Exception ex) {

                        log.debug(
                                "Unable to write GraphQL dump",
                                ex
                        );

                    }

                }

                BrowserResponse browserResponse =

                        BrowserResponse.builder()

                                .url(url)

                                .method(response.request().method())

                                .resourceType(resourceType)

                                .status(response.status())

                                .contentType(contentType)

                                .headers(response.headers())

                                .body(body)

                                .build();

                state.getResponses().add(browserResponse);

            }

            catch (Exception ex) {

                log.debug(
                        "Failed capturing response.",
                        ex
                );

            }

        });

    }

}