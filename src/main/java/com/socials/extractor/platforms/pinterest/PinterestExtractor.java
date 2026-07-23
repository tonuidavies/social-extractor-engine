package com.socials.extractor.platforms.pinterest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.socials.extractor.browser.BrowserCapture;
import com.socials.extractor.browser.BrowserClient;
import com.socials.extractor.core.Extractor;
import com.socials.extractor.model.BrowserSession;
import com.socials.extractor.model.ExtractionRequest;
import com.socials.extractor.model.ExtractionResponse;
import com.socials.extractor.model.MediaFormat;
import com.socials.extractor.model.MediaResult;
import com.socials.extractor.model.Platform;
import com.socials.extractor.support.BrowserMedia;
import com.socials.extractor.support.Extractions;
import com.socials.extractor.support.logging.AppLogger;
import lombok.RequiredArgsConstructor;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Pinterest extractor via the shared Playwright browser. Tries several strategies
 * (rendered {@code __PWS_DATA__}, Open Graph video meta, and a regex sweep of the
 * page + network for a pinimg .mp4), with an image fallback.
 */
@Component
@RequiredArgsConstructor
public class PinterestExtractor implements Extractor {

	private static final AppLogger log = AppLogger.of(PinterestExtractor.class);
	private static final Pattern PIN_ID = Pattern.compile("pin/(\\d+)");
	private static final String CHROME =
			"Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 "
					+ "(KHTML, like Gecko) Chrome/138.0.0.0 Safari/537.36";

	private final BrowserClient browserClient;
	private final ObjectMapper mapper;

	@Override
	public boolean supports(String url) {
		if (url == null) return false;
		String u = url.toLowerCase();
		return u.contains("pinterest.") || u.contains("pin.it");
	}

	@Override
	public Mono<ExtractionResponse> extract(ExtractionRequest request) {
		String url = request.getUrl();
		long t0 = System.currentTimeMillis();
		log.extractionStart(Platform.PINTEREST, url);

		return browserClient.capture(canonical(url), session())
				.map(capture -> build(capture, url, t0))
				.onErrorResume(e -> {
					log.extractionFailure(Platform.PINTEREST, url, e);
					return Mono.just(Extractions.fail("Couldn't load this pin."));
				});
	}

	private String canonical(String url) {
		Matcher m = PIN_ID.matcher(url);
		return m.find() ? "https://www.pinterest.com/pin/" + m.group(1) + "/" : url;
	}

	private BrowserSession session() {
		return BrowserSession.builder()
				.userAgent(CHROME)
				.acceptLanguage("en-US,en;q=0.9")
				.acceptEncoding("identity")
				.referer("https://www.pinterest.com/")
				.build();
	}

	private ExtractionResponse build(BrowserCapture capture, String url, long t0) {
		String html = capture.getHtml() == null ? "" : capture.getHtml();
		String text = BrowserMedia.scanText(capture);
		Document doc = Jsoup.parse(html);

		List<MediaFormat> formats = new ArrayList<>();
		String image = null;
		String title = BrowserMedia.ogContent(doc, "og:title", "twitter:title");

		// 1) __PWS_DATA__ blob
		try {
			Element script = doc.selectFirst("script#__PWS_DATA__");
			if (script == null) script = doc.selectFirst("script#initial-state");
			if (script != null && !script.data().isBlank()) {
				JsonNode root = mapper.readTree(script.data());
				if (title == null) title = firstText(root, "grid_title", "seo_title", "title");
				JsonNode orig = findKey(root, "orig");
				if (orig != null) image = orig.path("url").asText(null);
				JsonNode videoList = findKey(root, "video_list");
				if (videoList != null && videoList.isObject()) {
					var it = videoList.fields();
					while (it.hasNext()) {
						JsonNode r = it.next().getValue();
						String vurl = r.path("url").asText(null);
						if (vurl != null && vurl.contains(".mp4")) {
							Integer w = r.hasNonNull("width") ? r.get("width").asInt() : null;
							Integer h = r.hasNonNull("height") ? r.get("height").asInt() : null;
							formats.add(MediaFormat.builder().url(vurl).mimeType("video/mp4")
									.width(w).height(h).quality(Extractions.quality(h)).build());
							break;
						}
					}
				}
			}
		} catch (Exception ignore) {
		}

		// 2) Open Graph video
		if (formats.isEmpty()) {
			String og = BrowserMedia.ogContent(doc, "og:video", "og:video:url", "og:video:secure_url");
			if (og != null && og.contains(".mp4")) {
				formats.add(MediaFormat.builder().url(og).mimeType("video/mp4").quality("HD").build());
			}
		}

		// 3) Regex sweep of page + network for a pinimg .mp4
		if (formats.isEmpty()) {
			String mp4 = BrowserMedia.firstMp4(text, "pinimg.com", "pinterest.com");
			if (mp4 != null) {
				formats.add(MediaFormat.builder().url(mp4).mimeType("video/mp4").quality("HD").build());
			}
		}

		// 4) Image fallback
		if (image == null) image = BrowserMedia.ogContent(doc, "og:image", "twitter:image");
		if (formats.isEmpty() && image != null) {
			formats.add(MediaFormat.builder().url(image).mimeType("image/jpeg").quality("Image").build());
		}

		if (formats.isEmpty()) {
			boolean login = html.toLowerCase().contains("log in") || html.length() < 2000;
			log.warn("[pinterest] no media (htmlLen={} login={}) url={}",
					html.length(), login, AppLogger.safeUrl(url));
			return Extractions.fail(login
					? "Pinterest required a login for this pin, so it can't be saved."
					: "No downloadable media found on this pin.");
		}

		MediaResult result = MediaResult.builder()
				.platform(Platform.PINTEREST)
				.title(title)
				.thumbnail(image)
				.url(formats.get(0).getUrl())
				.formats(formats)
				.build();

		log.extractionSuccess(Platform.PINTEREST, url, formats.size(), System.currentTimeMillis() - t0);
		return Extractions.ok(result);
	}

	private String firstText(JsonNode root, String... keys) {
		for (String k : keys) {
			JsonNode n = findKey(root, k);
			if (n != null && n.isTextual() && !n.asText().isBlank()) return n.asText();
		}
		return null;
	}

	private JsonNode findKey(JsonNode node, String key) {
		if (node == null) return null;
		if (node.isObject()) {
			if (node.has(key)) return node.get(key);
			var it = node.elements();
			while (it.hasNext()) {
				JsonNode r = findKey(it.next(), key);
				if (r != null) return r;
			}
		} else if (node.isArray()) {
			for (JsonNode c : node) {
				JsonNode r = findKey(c, key);
				if (r != null) return r;
			}
		}
		return null;
	}
}