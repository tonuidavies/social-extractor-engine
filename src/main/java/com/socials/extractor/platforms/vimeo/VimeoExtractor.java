package com.socials.extractor.platforms.vimeo;

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
 * Vimeo extractor via the player config JSON (progressive MP4 renditions).
 * No auth, no browser.
 *
 * <p>Some videos are private or domain-restricted; the config endpoint then
 * returns a non-video payload and we report "not available". HLS-only videos
 * (no progressive files) are also reported as unavailable for direct download.
 */
@Component
@RequiredArgsConstructor
public class VimeoExtractor implements Extractor {

	private static final AppLogger log = AppLogger.of(VimeoExtractor.class);
	private static final Pattern VIDEO_ID = Pattern.compile("vimeo\\.com/(?:video/)?(\\d+)");

	private final HttpClient http;
	private final ObjectMapper mapper;

	@Override
	public boolean supports(String url) {
		return url != null && url.toLowerCase().contains("vimeo.com");
	}

	@Override
	public Mono<ExtractionResponse> extract(ExtractionRequest request) {
		String url = request.getUrl();
		long t0 = System.currentTimeMillis();

		Matcher m = VIDEO_ID.matcher(url);
		if (!m.find()) {
			return Mono.just(Extractions.fail("Couldn't find a Vimeo video id in that link."));
		}
		String id = m.group(1);
		log.extractionStart(Platform.VIMEO, url);

		String config = "https://player.vimeo.com/video/" + id + "/config";
		return http.get(HttpRequest.builder().url(config).headers(headers()).build())
				.map(resp -> parse(resp.getBody(), url, t0))
				.onErrorResume(e -> {
					log.extractionFailure(Platform.VIMEO, url, e);
					return Mono.just(Extractions.fail("This Vimeo video isn't available for download."));
				});
	}

	private Map<String, String> headers() {
		Map<String, String> h = new LinkedHashMap<>();
		h.put("User-Agent", Extractions.USER_AGENT);
		h.put("Referer", "https://vimeo.com/");
		h.put("Accept", "application/json");
		return h;
	}

	private ExtractionResponse parse(String body, String url, long t0) {
		try {
			JsonNode root = mapper.readTree(body);
			JsonNode progressive = root.path("request").path("files").path("progressive");
			if (!progressive.isArray() || progressive.size() == 0) {
				return Extractions.fail("This Vimeo video has no downloadable file (HLS-only or private).");
			}

			List<MediaFormat> formats = new ArrayList<>();
			for (JsonNode f : progressive) {
				String furl = f.path("url").asText(null);
				if (furl == null) continue;
				Integer w = f.hasNonNull("width") ? f.get("width").asInt() : null;
				Integer hgt = f.hasNonNull("height") ? f.get("height").asInt() : null;
				formats.add(MediaFormat.builder()
						.url(furl)
						.mimeType("video/mp4")
						.width(w)
						.height(hgt)
						.quality(f.path("quality").asText(Extractions.quality(hgt)))
						.build());
			}
			if (formats.isEmpty()) {
				return Extractions.fail("This Vimeo video has no downloadable file.");
			}

			JsonNode video = root.path("video");
			MediaResult result = MediaResult.builder()
					.platform(Platform.VIMEO)
					.title(video.path("title").asText(null))
					.thumbnail(bestThumb(video.path("thumbs")))
					.url(formats.get(0).getUrl())
					.duration(video.hasNonNull("duration") ? video.get("duration").asLong() : null)
					.formats(formats)
					.build();

			log.extractionSuccess(Platform.VIMEO, url, formats.size(), System.currentTimeMillis() - t0);
			return Extractions.ok(result);
		} catch (Exception e) {
			log.extractionFailure(Platform.VIMEO, url, e);
			return Extractions.fail("Couldn't parse this Vimeo video.");
		}
	}

	private String bestThumb(JsonNode thumbs) {
		if (thumbs.isMissingNode()) return null;
		// thumbs is an object like { "640": "...", "960": "...", "base": "..." }
		String best = null;
		int bestKey = -1;
		var it = thumbs.fields();
		while (it.hasNext()) {
			var e = it.next();
			String key = e.getKey();
			String val = e.getValue().asText(null);
			if (val == null) continue;
			try {
				int k = Integer.parseInt(key);
				if (k > bestKey) {
					bestKey = k;
					best = val;
				}
			} catch (NumberFormatException nfe) {
				if (best == null) best = val; // "base"
			}
		}
		return best;
	}
}
