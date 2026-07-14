package com.socials.extractor.network.transport;

import com.socials.extractor.model.BrowserSession;
import reactor.core.publisher.Mono;

public interface BrowserTransport {

    Mono<BrowserResponse> get(
            String url,
            BrowserSession session
    );

}