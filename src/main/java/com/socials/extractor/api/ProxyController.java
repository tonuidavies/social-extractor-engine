package com.socials.extractor.api;

import com.socials.extractor.browser.BrowserClient;
import com.socials.extractor.model.BrowserSession;
import com.socials.extractor.network.ProxyDiagnostics;
import com.socials.extractor.network.ProxySettings;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Diagnostics for the outgoing proxy IP.
 *
 * <ul>
 *   <li>{@code GET /api/v1/proxy/ip} — the IP for the plain HTTP path.</li>
 *   <li>{@code GET /api/v1/proxy/ip/browser} — the IP Playwright captures go out
 *       on (usually a DIFFERENT Webshare IP than the HTTP path).</li>
 * </ul>
 *
 * <p>Call {@code /ip} a few times in a row to watch the IP rotate.
 */
@RestController
@RequestMapping("/api/v1/proxy")
@RequiredArgsConstructor
@CrossOrigin("*")
public class ProxyController {

	private static final Pattern IPV4 = Pattern.compile("\\b\\d{1,3}(?:\\.\\d{1,3}){3}\\b");

	private final ProxyDiagnostics diagnostics;
	private final ProxySettings proxy;
	private final BrowserClient browserClient;

	@GetMapping("/ip")
	public Mono<Map<String, Object>> httpIp() {
		if (!proxy.isEnabled()) {
			return Mono.just(result("http", null, "proxy disabled"));
		}
		return diagnostics.outboundIp()
				.map(ip -> result("http", ip, null))
				.onErrorResume(e -> Mono.just(result("http", null, e.getMessage())));
	}

	@GetMapping("/ip/browser")
	public Mono<Map<String, Object>> browserIp() {
		if (!proxy.isEnabled()) {
			return Mono.just(result("browser", null, "proxy disabled"));
		}
		return browserClient.capture("https://api.ipify.org", session())
				.map(cap -> {
					Matcher m = IPV4.matcher(cap.getHtml() == null ? "" : cap.getHtml());
					String ip = m.find() ? m.group() : null;
					return result("browser", ip, ip == null ? "no IP in page" : null);
				})
				.onErrorResume(e -> Mono.just(result("browser", null, e.getMessage())));
	}

	private BrowserSession session() {
		return BrowserSession.builder()
				.userAgent("Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 "
						+ "(KHTML, like Gecko) Chrome/138.0.0.0 Safari/537.36")
				.acceptLanguage("en-US,en;q=0.9")
				.acceptEncoding("identity")
				.build();
	}

	private Map<String, Object> result(String egress, String ip, String error) {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("egress", egress);
		m.put("proxyEnabled", proxy.isEnabled());
		m.put("ip", ip);
		if (error != null) m.put("error", error);
		return m;
	}
}