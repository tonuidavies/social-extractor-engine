package com.socials.extractor.network.http;

import com.socials.extractor.network.ProxySettings;
import io.netty.channel.ChannelOption;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClientResponse;
import reactor.netty.resources.ConnectionProvider;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;

/**
 * Reactor-Netty HTTP client — pooled, timed out, and routed through the Webshare
 * rotating proxy when enabled (see {@link ProxySettings}). Used by the JSON/HTML
 * extractors (Twitter, Reddit manifest, Vimeo, …).
 */
@Component
public class ReactorHttpClient
		implements com.socials.extractor.network.http.HttpClient {

	private static final ConnectionProvider POOL = ConnectionProvider.builder("ext-http")
			.maxConnections(64)
			.pendingAcquireTimeout(Duration.ofSeconds(10))
			.maxIdleTime(Duration.ofSeconds(30))
			.build();

	private final reactor.netty.http.client.HttpClient client;

	public ReactorHttpClient(ProxySettings proxy) {
		reactor.netty.http.client.HttpClient base =
				reactor.netty.http.client.HttpClient.create(POOL)
						.compress(true)
						.followRedirect(true)
						.responseTimeout(Duration.ofSeconds(20))
						.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10_000);
		this.client = proxy.applyReactorProxy(base);
	}

	@Override
	public Mono<HttpResponse> get(HttpRequest request) {
		return client
				.headers(headers -> request.getHeaders().forEach(headers::set))
				.get()
				.uri(request.getUrl())
				.responseSingle((HttpClientResponse response, reactor.netty.ByteBufMono body) ->
						body.asString(StandardCharsets.UTF_8)
								.defaultIfEmpty("")
								.map(html -> HttpResponse.builder()
										.status(response.status().code())
										.body(html)
										.headers(new HashMap<>())
										.finalUrl(request.getUrl())
										.build()));
	}
}