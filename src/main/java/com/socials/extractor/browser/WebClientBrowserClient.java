package com.socials.extractor.browser;

import com.socials.extractor.model.BrowserSession;
import com.socials.extractor.network.WebClientFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class WebClientBrowserClient
        implements BrowserClient {

    private final WebClientFactory factory;

    @Override
    public Mono<BrowserCapture> capture(
            String url,
            BrowserSession session
    ) {

        WebClient client =
                factory.client(session);

        return client

                .get()

                .uri(url)

                .exchangeToMono(response ->

                        response

                                .bodyToMono(String.class)

                                .map(body ->

                                        BrowserCapture

                                                .builder()

                                                .finalUrl(url)

                                                .html(body)

                                                .build()

                                )

                );

    }

}