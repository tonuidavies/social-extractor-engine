package com.socials.extractor.network.http;

import reactor.core.publisher.Mono;

public interface HttpClient {

    Mono<HttpResponse> get(
            HttpRequest request
    );

}