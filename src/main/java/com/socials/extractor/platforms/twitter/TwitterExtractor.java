package com.socials.extractor.platforms.twitter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.socials.extractor.core.Extractor;
import com.socials.extractor.model.ExtractionRequest;
import com.socials.extractor.model.ExtractionResponse;
import com.socials.extractor.model.MediaFormat;
import com.socials.extractor.model.MediaResult;
import com.socials.extractor.model.Platform;
import com.socials.extractor.network.http.HttpClient;
import com.socials.extractor.network.http.HttpRequest;
import com.socials.extractor.support.Extractions;
import com.socials.extractor.support.logging.AppLogger;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Twitter / X extractor with resilience against a single provider being down.
 *
 * <p>Tries fxtwitter first; if it errors or returns no media, falls back to
 * vxtwitter. Both are community JSON mirrors that return direct MP4/photo URLs
 * without auth. If both are unavailable, reports a clear message.
 */
@Component
@RequiredArgsConstructor
public class TwitterExtractor implements Extractor {

	private static final AppLogger log = AppLogger.of(TwitterExtractor.class);
	private static final Pattern TWEET_ID = Pattern.compile("status(?:es)?/(\\d+)");

	private final HttpClient http;
	private final ObjectMapper mapper;

	@Override
	public boolean supports(String url) {
		if (url == null) return false;
		String u = url.toLowerCase();
		return u.contains("twitter.com") || u.contains("://x.com") || u.contains(".x.com");
	}

	@Override
	public Mono<ExtractionResponse> extract(ExtractionRequest request) {
		String url = request.getUrl();
		long t0 = System.currentTimeMillis();

		Matcher m = TWEET_ID.matcher(url);
		if (!m.find()) {
			return Mono.just(Extractions.fail("Couldn't find a tweet id in that link."));
		}
		String id = m.group(1);
		log.extractionStart(Platform.TWITTER, url);

		// fxtwitter first, then vxtwitter as a fallback.
		return body("https://api.fxtwitter.com/i/status/" + id)
				.map(b -> parseFx(b, url, t0))
				.onErrorResume(e -> {
					log.warn("[twitter] fxtwitter failed: {}", e.getMessage());
					return Mono.just(Extractions.fail(""));
				})
				.flatMap(r -> r.isSuccess()
						? Mono.just(r)
						: body("https://api.vxtwitter.com/Twitter/status/" + id)
						.map(b -> parseVx(b, url, t0))
						.onErrorResume(e -> {
							log.extractionFailure(Platform.TWITTER, url, e);
							return Mono.just(Extractions.fail(
									"Couldn't read this tweet (both providers unavailable)."));
						}));
	}

	private Mono<String> body(String apiUrl) {
		return http.get(HttpRequest.builder().url(apiUrl).headers(headers()).build())
				.map(resp -> resp.getBody());
	}

	private Map<String, String> headers() {
		Map<String, String> h = new LinkedHashMap<>();
		h.put("User-Agent", Extractions.USER_AGENT);
		h.put("Accept", "application/json");
		return h;
	}

	/* ---------------- fxtwitter ---------------- */

	private ExtractionResponse parseFx(String body, String url, long t0) {
		try {
			JsonNode tweet = mapper.readTree(body).path("tweet");
			if (tweet.isMissingNode() || tweet.isNull()) return Extractions.fail("");

			List<MediaFormat> formats = new ArrayList<>();
			JsonNode media = tweet.path("media");
			for (JsonNode v : media.path("videos")) {
				addVideo(formats, v.path("url").asText(null),
						intOf(v, "width"), intOf(v, "height"));
			}
			for (JsonNode p : media.path("photos")) {
				addImage(formats, p.path("url").asText(null));
			}
			if (formats.isEmpty()) return Extractions.fail("");

			String thumb = media.path("videos").path(0).path("thumbnail_url").asText(null);
			if (thumb == null) thumb = media.path("photos").path(0).path("url").asText(null);

			return ok(formats, tweet.path("text").asText(null), thumb, url, t0);
		} catch (Exception e) {
			return Extractions.fail("");
		}
	}

	/* ---------------- vxtwitter ---------------- */

	private ExtractionResponse parseVx(String body, String url, long t0) {
		try {
			JsonNode root = mapper.readTree(body);
			List<MediaFormat> formats = new ArrayList<>();
			String thumb = null;

			for (JsonNode mm : root.path("media_extended")) {
				String type = mm.path("type").asText("");
				String murl = mm.path("url").asText(null);
				if (murl == null) continue;
				if (type.equals("image")) {
					addImage(formats, murl);
				} else { // video or gif
					addVideo(formats, murl, intOf(mm, "width"), intOf(mm, "height"));
					if (thumb == null) thumb = mm.path("thumbnail_url").asText(null);
				}
			}
			if (formats.isEmpty()) {
				return Extractions.fail("No video or image found in this tweet.");
			}
			return ok(formats, root.path("text").asText(null), thumb, url, t0);
		} catch (Exception e) {
			return Extractions.fail("Couldn't read this tweet.");
		}
	}

	/* ---------------- shared ---------------- */

	private void addVideo(List<MediaFormat> formats, String url, Integer w, Integer h) {
		if (url == null) return;
		formats.add(MediaFormat.builder()
				.url(url).mimeType("video/mp4").width(w).height(h)
				.quality(Extractions.quality(h)).build());
	}

	private void addImage(List<MediaFormat> formats, String url) {
		if (url == null) return;
		formats.add(MediaFormat.builder()
				.url(url).mimeType("image/jpeg").quality("Image").build());
	}

	private ExtractionResponse ok(List<MediaFormat> formats, String title, String thumb,
								  String url, long t0) {
		MediaResult result = MediaResult.builder()
				.platform(Platform.TWITTER)
				.title(title)
				.thumbnail(thumb)
				.url(formats.get(0).getUrl())
				.formats(formats)
				.build();
		log.extractionSuccess(Platform.TWITTER, url, formats.size(), System.currentTimeMillis() - t0);
		return Extractions.ok(result);
	}

	private Integer intOf(JsonNode n, String field) {
		return n.hasNonNull(field) ? n.get(field).asInt() : null;
	}
}