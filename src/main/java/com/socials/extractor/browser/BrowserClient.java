package com.socials.extractor.browser;

import com.socials.extractor.model.BrowserSession;
import reactor.core.publisher.Mono;

public interface BrowserClient {

    Mono<BrowserCapture> capture(
            String url,
            BrowserSession session
    );

}