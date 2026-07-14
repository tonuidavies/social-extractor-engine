package com.socials.extractor.network.transport;

import com.socials.extractor.model.BrowserSession;
import io.netty.handler.codec.http.HttpHeaderNames;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;

@Component
@RequiredArgsConstructor
public class ReactorNettyBrowserTransport
        implements BrowserTransport {

    @Override
    public Mono<BrowserResponse> get(
            String url,
            BrowserSession session
    ) {

        return HttpClient

                .create()

                .compress(true)

                .followRedirect(true)

                .headers(headers -> {

                    headers.set(
                            HttpHeaderNames.USER_AGENT,
                            session.getUserAgent()
                    );

                    headers.set(
                            HttpHeaderNames.ACCEPT_LANGUAGE,
                            session.getAcceptLanguage()
                    );

                    headers.set(
                            HttpHeaderNames.REFERER,
                            session.getReferer()
                    );

                })

                .get()

                .uri(url)

                .responseSingle(

                        (response, body) ->

                                body

                                        .asString(StandardCharsets.UTF_8)

                                        .map(html ->

                                                BrowserResponse

                                                        .builder()

                                                        .status(
                                                                response.status().code()
                                                        )

                                                        .body(html)

                                                        .headers(new HashMap<>())

                                                        .finalUrl(url)

                                                        .build()

                                        )

                );

    }

}