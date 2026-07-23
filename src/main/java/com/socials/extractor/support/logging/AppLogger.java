package com.socials.extractor.support.logging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.util.function.Supplier;

/**
 * Reusable logging facade over SLF4J.
 *
 * <p>Goals:
 * <ul>
 *   <li>One consistent way to log across the whole extractor (levels, timing, context).</li>
 *   <li>Automatic redaction of signed CDN URLs — they carry signatures/tokens you do
 *       not want in your logs.</li>
 *   <li>Domain helpers for the extraction lifecycle so logs read the same everywhere.</li>
 * </ul>
 *
 * <p>Usage:
 * <pre>
 *   private static final AppLogger log = AppLogger.of(MyClass.class);
 *   log.info("fetching {}", AppLogger.safeUrl(url));
 *   log.extractionStart(platform, url);
 *   MediaResult r = log.timed("resolve", () -> resolver.resolve(capture));
 * </pre>
 *
 * <p>Reactive note: SLF4J MDC does not automatically propagate across Reactor
 * threads. For per-request correlation ids in WebFlux, set them inside the
 * reactive chain (e.g. {@code doOnEach}) rather than relying on thread-locals.
 */
public final class AppLogger {

	private final Logger log;

	private AppLogger(Class<?> type) {
		this.log = LoggerFactory.getLogger(type);
	}

	private AppLogger(String name) {
		this.log = LoggerFactory.getLogger(name);
	}

	public static AppLogger of(Class<?> type) {
		return new AppLogger(type);
	}

	public static AppLogger of(String name) {
		return new AppLogger(name);
	}

	/* ---------------- basic levels ---------------- */

	public void info(String msg, Object... args) {
		log.info(msg, args);
	}

	public void debug(String msg, Object... args) {
		if (log.isDebugEnabled()) log.debug(msg, args);
	}

	public void warn(String msg, Object... args) {
		log.warn(msg, args);
	}

	public void error(String msg, Object... args) {
		log.error(msg, args);
	}

	public void error(String msg, Throwable t) {
		log.error(msg, t);
	}

	public boolean isDebug() {
		return log.isDebugEnabled();
	}

	/* ---------------- extraction lifecycle ---------------- */

	public void extractionStart(Object platform, String url) {
		log.info("[extract:start] platform={} url={}", platform, safeUrl(url));
	}

	public void extractionSuccess(Object platform, String url, int formatCount, long ms) {
		log.info(
				"[extract:ok] platform={} formats={} took={}ms url={}",
				platform,
				formatCount,
				ms,
				safeUrl(url));
	}

	public void extractionFailure(Object platform, String url, Throwable t) {
		log.warn(
				"[extract:fail] platform={} url={} reason={}",
				platform,
				safeUrl(url),
				t.getMessage());
		if (log.isDebugEnabled()) log.debug("[extract:fail] stacktrace", t);
	}

	/* ---------------- timing ---------------- */

	/** Time a blocking section and log it at INFO. */
	public <T> T timed(String label, Supplier<T> action) {
		long start = System.nanoTime();
		try {
			return action.get();
		} finally {
			log.info("[timing] {} took {}ms", label, (System.nanoTime() - start) / 1_000_000);
		}
	}

	/* ---------------- context (MDC) ---------------- */

	public static void putContext(String key, String value) {
		if (key != null && value != null) MDC.put(key, value);
	}

	public static void clearContext() {
		MDC.clear();
	}

	/* ---------------- redaction ---------------- */

	/**
	 * Strip the query string (signatures, tokens, expiry) from a URL so logs stay
	 * clean and don't leak short-lived credentials. Keeps scheme, host and path.
	 */
	public static String safeUrl(String url) {
		if (url == null || url.isBlank()) return "<none>";
		int q = url.indexOf('?');
		String base = q >= 0 ? url.substring(0, q) : url;
		// Cap very long paths too.
		return base.length() > 160 ? base.substring(0, 160) + "…" : base;
	}
}