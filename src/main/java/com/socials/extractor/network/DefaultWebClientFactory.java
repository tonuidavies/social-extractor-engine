package com.socials.extractor.network;

import com.socials.extractor.model.BrowserSession;
import org.springframework.http.HttpHeaders;
import org.springframework.http.codec.support.DefaultClientCodecConfigurer;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;

@Component
public class DefaultWebClientFactory
        implements WebClientFactory {

    @Override
    public WebClient client(
            BrowserSession session
    ) {

        ExchangeStrategies strategies =

                ExchangeStrategies.builder()

                        .codecs(codecs ->

                                codecs.defaultCodecs()

                                        .maxInMemorySize(
                                                10 * 1024 * 1024
                                        )

                        )

                        .build();

        return WebClient.builder()

                .exchangeStrategies(strategies)

                .defaultHeader(
                        HttpHeaders.USER_AGENT,
                        session.getUserAgent()
                )

                .defaultHeader(
                        HttpHeaders.ACCEPT,
                        "text/html,application/xhtml+xml"
                )

                .defaultHeader(
                        HttpHeaders.ACCEPT_LANGUAGE,
                        session.getAcceptLanguage()
                )

                .defaultHeader(
                        HttpHeaders.ACCEPT_ENCODING,
                        session.getAcceptEncoding()
                )

                .defaultHeader(
                        HttpHeaders.REFERER,
                        session.getReferer()
                )

                .build();

    }

}