package com.socials.extractor.network;

import com.socials.extractor.model.BrowserSession;
import org.springframework.web.reactive.function.client.WebClient;

public interface WebClientFactory {

    WebClient client(
            BrowserSession session
    );

}