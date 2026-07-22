package com.socials.extractor.proxy;

import io.netty.channel.ChannelOption;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;

/**
 * Dedicated {@link WebClient} for the media proxy.
 *
 * <p>Kept separate from any application WebClient so we can tune it purely for
 * streaming large binaries: follow redirects (CDNs love 302s), no automatic
 * decompression (we forward bytes verbatim), and generous timeouts.
 */
@Configuration
public class MediaProxyConfig {

    @Bean
    public WebClient mediaProxyWebClient() {

        HttpClient httpClient = HttpClient.create()
                .followRedirect(true)
                .compress(false) // ask for identity; we stream raw bytes downstream
                .responseTimeout(Duration.ofSeconds(60))
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 15_000);

        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                // We never buffer the body into memory (it is streamed), so this
                // only guards non-streaming paths; keep it high just in case.
                .codecs(c -> c.defaultCodecs().maxInMemorySize(16 * 1024 * 1024))
                .build();
    }
}
