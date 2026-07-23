package com.socials.extractor.proxy;

import com.socials.extractor.network.ProxySettings;
import io.netty.channel.ChannelOption;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;

/**
 * Dedicated {@link WebClient} for the media download proxy.
 *
 * <p>Routed through the Webshare proxy too, so downloads come from rotating IPs —
 * unless {@code proxy.stream-downloads=false}, which keeps the (bandwidth-heavy)
 * video bytes off the proxy while still proxying extraction. Videos are large, so
 * proxying downloads is the expensive part of a residential-proxy bill; this flag
 * lets you separate the two.
 */
@Configuration
public class MediaProxyConfig {

    @Bean
    public WebClient mediaProxyWebClient(
            ProxySettings proxy,
            @Value("${proxy.stream-downloads:true}") boolean streamDownloads) {

        HttpClient httpClient = HttpClient.create()
                .followRedirect(true)
                .compress(false)
                .responseTimeout(Duration.ofSeconds(60))
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 15_000);

        if (streamDownloads) {
            httpClient = proxy.applyReactorProxy(httpClient);
        }

        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .codecs(c -> c.defaultCodecs().maxInMemorySize(16 * 1024 * 1024))
                .build();
    }
}