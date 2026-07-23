package com.socials.extractor.network;

import com.socials.extractor.support.logging.AppLogger;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;

import java.time.Duration;

/**
 * Reports the outbound IP the Webshare proxy is currently giving us.
 *
 * <p>Uses a NON-pooled client (fresh connection per call), so each IP check goes
 * through a new proxy connection — which is exactly when Webshare's rotating
 * endpoint hands out a new IP. That makes repeated calls actually show rotation,
 * unlike the pooled extraction client which reuses a connection (and its IP)
 * until it recycles.
 */
@Component
public class ProxyDiagnostics {

	private static final AppLogger log = AppLogger.of(ProxyDiagnostics.class);

	private final ProxySettings proxy;

	@Value("${proxy.ip-check-url:https://api.ipify.org}")
	private String ipCheckUrl;

	private WebClient client;

	public ProxyDiagnostics(ProxySettings proxy) {
		this.proxy = proxy;
	}

	@PostConstruct
	public void init() {
		HttpClient hc = HttpClient.create(ConnectionProvider.newConnection())
				.followRedirect(true)
				.responseTimeout(Duration.ofSeconds(10));
		hc = proxy.applyReactorProxy(hc);
		this.client = WebClient.builder()
				.clientConnector(new ReactorClientHttpConnector(hc))
				.build();
	}

	/** The outbound IP for the plain HTTP path (Twitter/Reddit-manifest/Vimeo/etc.). */
	public Mono<String> outboundIp() {
		return client.get()
				.uri(ipCheckUrl)
				.retrieve()
				.bodyToMono(String.class)
				.map(String::trim);
	}

	@EventListener(ApplicationReadyEvent.class)
	public void logAtStartup() {
		if (!proxy.isEnabled()) {
			log.info("[proxy] disabled — traffic goes out on the server's own IP");
			return;
		}
		outboundIp().subscribe(
				ip -> log.info("[proxy] outbound IP via Webshare = {}", ip),
				err -> log.warn("[proxy] IP check failed: {}", err.getMessage()));
	}
}