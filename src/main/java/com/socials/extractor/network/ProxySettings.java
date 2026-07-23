package com.socials.extractor.network;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import reactor.netty.http.client.HttpClient;
import reactor.netty.transport.ProxyProvider;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Central proxy configuration (Webshare rotating proxy).
 *
 * <p>One switch controls every outbound path — the reactor HTTP clients (Twitter,
 * Reddit manifest, Vimeo…), the media download streamer, the Playwright browser,
 * and the ffmpeg process for Reddit muxing.
 *
 * <p>application.properties:
 * <pre>
 *   proxy.enabled=true
 *   proxy.host=p.webshare.io      # your Webshare rotating endpoint host
 *   proxy.port=80                 # your Webshare rotating endpoint port
 *   proxy.username=YOUR_USERNAME  # Webshare proxy username
 *   proxy.password=YOUR_PASSWORD  # Webshare proxy password
 *   # Optional: keep large video downloads OFF the proxy to save bandwidth $$
 *   proxy.stream-downloads=true
 * </pre>
 *
 * <p>Webshare's rotating endpoint gives a fresh IP per connection automatically,
 * which fits our per-request pattern — no session handling needed here.
 */
@Component
public class ProxySettings {

	@Value("${proxy.enabled:false}")
	private boolean enabled;

	@Getter
    @Value("${proxy.host:}")
	private String host;

	@Getter
    @Value("${proxy.port:0}")
	private int port;

	@Getter
    @Value("${proxy.username:}")
	private String username;

	@Getter
    @Value("${proxy.password:}")
	private String password;

	public boolean isEnabled() {
		return enabled && host != null && !host.isBlank() && port > 0;
	}

    /** Apply the proxy to a reactor-netty HttpClient (no-op if disabled). */
	public HttpClient applyReactorProxy(HttpClient client) {
		if (!isEnabled()) {
			return client;
		}
		return client.proxy(spec -> {
			ProxyProvider.Builder b = spec
					.type(ProxyProvider.Proxy.HTTP)
					.host(host)
					.port(port);
			if (username != null && !username.isBlank()) {
				b.username(username).password(u -> password);
			}
		});
	}

	/**
	 * Proxy URL for CLI tools that read {@code http_proxy}/{@code https_proxy}
	 * (used to route ffmpeg's fetches through the proxy). Null if disabled.
	 */
	public String proxyEnvUrl() {
		if (!isEnabled()) {
			return null;
		}
		String cred = "";
		if (username != null && !username.isBlank()) {
			cred = URLEncoder.encode(username, StandardCharsets.UTF_8)
					+ ":" + URLEncoder.encode(password == null ? "" : password, StandardCharsets.UTF_8)
					+ "@";
		}
		return "http://" + cred + host + ":" + port;
	}
}