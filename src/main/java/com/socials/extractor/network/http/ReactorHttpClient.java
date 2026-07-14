package com.socials.extractor.network.http;

import io.netty.handler.codec.http.HttpHeaderNames;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClientResponse;
import reactor.netty.http.client.HttpClient;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;

@Component
@RequiredArgsConstructor
public class ReactorHttpClient
        implements com.socials.extractor.network.http.HttpClient {

    @Override
    public Mono<HttpResponse> get(
            HttpRequest request
    ) {

        HttpClient client =

                HttpClient.create()

                        .compress(true)

                        .followRedirect(true);

        return client

                .headers(headers ->

                        request.getHeaders()

                                .forEach(headers::set)

                )

                .get()

                .uri(request.getUrl())

                .responseSingle(

                        (HttpClientResponse response, reactor.netty.ByteBufMono body) ->

                                body.asString(StandardCharsets.UTF_8)

                                        .map(html ->

                                                HttpResponse.builder()

                                                        .status(
                                                                response.status().code()
                                                        )

                                                        .body(html)

                                                        .headers(new HashMap<>())

                                                        .finalUrl(request.getUrl())

                                                        .build()

                                        )

                );

    }

}