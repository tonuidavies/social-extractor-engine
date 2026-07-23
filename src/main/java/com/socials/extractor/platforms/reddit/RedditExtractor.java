package com.socials.extractor.platforms.reddit;

import com.socials.extractor.browser.BrowserClient;
import com.socials.extractor.browser.BrowserCapture;
import com.socials.extractor.core.Extractor;
import com.socials.extractor.model.BrowserSession;
import com.socials.extractor.model.ExtractionRequest;
import com.socials.extractor.model.ExtractionResponse;
import com.socials.extractor.model.MediaFormat;
import com.socials.extractor.model.MediaResult;
import com.socials.extractor.model.Platform;
import com.socials.extractor.network.http.HttpClient;
import com.socials.extractor.network.http.HttpRequest;
import com.socials.extractor.support.BrowserMedia;
import com.socials.extractor.support.Extractions;
import com.socials.extractor.support.logging.AppLogger;
import lombok.RequiredArgsConstructor;
import org.jsoup.Jsoup;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reddit extractor — NO API credentials required.
 *
 * <p>Reddit blocks anonymous {@code .json} from server IPs, but serves the normal
 * post page to a real browser. So we render the page with the shared Playwright
 * browser, find the {@code v.redd.it} id, then read the DASH manifest directly
 * from {@code v.redd.it} (a media CDN that is NOT IP-blocked) to get the actual
 * video URL. The proxy's {@link com.socials.extractor.proxy.RedditMuxer} then
 * adds the separate audio track at download time.
 */
@Component
@RequiredArgsConstructor
public class RedditExtractor implements Extractor {

	private static final AppLogger log = AppLogger.of(RedditExtractor.class);
	private static final String CHROME =
			"Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 "
					+ "(KHTML, like Gecko) Chrome/138.0.0.0 Safari/537.36";

	private static final Pattern VREDD_MP4 =
			Pattern.compile("https?://v\\.redd\\.it/[A-Za-z0-9_-]+/DASH_(\\d+)\\.mp4", Pattern.CASE_INSENSITIVE);
	private static final Pattern VREDD_ID =
			Pattern.compile("v\\.redd\\.it/([A-Za-z0-9_-]+)", Pattern.CASE_INSENSITIVE);
	private static final Pattern IREDD =
			Pattern.compile("https?://i\\.redd\\.it/[A-Za-z0-9_-]+\\.(?:jpg|jpeg|png|gif)", Pattern.CASE_INSENSITIVE);
	private static final Pattern MPD_BASEURL =
			Pattern.compile("DASH_(\\d+)\\.mp4");

	private final BrowserClient browserClient;
	private final HttpClient http;

	@Override
	public boolean supports(String url) {
		if (url == null) return false;
		String u = url.toLowerCase();
		return u.contains("reddit.com") || u.contains("redd.it");
	}

	@Override
	public Mono<ExtractionResponse> extract(ExtractionRequest request) {
		String url = request.getUrl();
		long t0 = System.currentTimeMillis();
		log.extractionStart(Platform.REDDIT, url);

		return browserClient.capture(url, session())
				.flatMap(capture -> resolve(capture, url, t0))
				.onErrorResume(e -> {
					log.extractionFailure(Platform.REDDIT, url, e);
					return Mono.just(Extractions.fail("Couldn't load this Reddit post."));
				});
	}

	private BrowserSession session() {
		return BrowserSession.builder()
				.userAgent(CHROME)
				.acceptLanguage("en-US,en;q=0.9")
				.acceptEncoding("identity")
				.referer("https://www.reddit.com/")
				.build();
	}

	private Mono<ExtractionResponse> resolve(BrowserCapture capture, String url, long t0) {
		String text = BrowserMedia.scanText(capture);
		String title = titleOf(capture);

		// 1) A DASH video URL already present in the capture — pick highest quality.
		String best = highestDash(BrowserMedia.matches(text, VREDD_MP4));
		if (best != null) {
			return Mono.just(video(best, title, url, t0));
		}

		// 2) Only the id present — read the DASH manifest from the open media CDN.
		Matcher id = VREDD_ID.matcher(text);
		if (id.find()) {
			String base = "https://v.redd.it/" + id.group(1) + "/";
			return fetchManifestVideo(base)
					.map(vurl -> video(vurl, title, url, t0))
					.switchIfEmpty(Mono.just(imageOrFail(text, title, url, t0)));
		}

		// 3) Image / gif post.
		return Mono.just(imageOrFail(text, title, url, t0));
	}

	private Mono<String> fetchManifestVideo(String base) {
		String mpd = base + "DASHPlaylist.mpd";
		return http.get(HttpRequest.builder()
						.url(mpd)
						.headers(Map.of("User-Agent", CHROME))
						.build())
				.map(resp -> {
					String body = resp.getBody() == null ? "" : resp.getBody();
					Matcher m = MPD_BASEURL.matcher(body); // DASH_<n>.mp4 (relative names)
					String bestName = null;
					int bestN = -1;
					while (m.find()) {
						int n = Integer.parseInt(m.group(1));
						if (n > bestN) {
							bestN = n;
							bestName = m.group();
						}
					}
					return bestName == null ? "" : base + bestName; // full URL
				})
				.filter(s -> !s.isEmpty())
				.onErrorResume(e -> Mono.empty());
	}

	private ExtractionResponse imageOrFail(String text, String title, String url, long t0) {
		List<String> imgs = BrowserMedia.matches(text, IREDD);
		if (!imgs.isEmpty()) {
			MediaResult result = MediaResult.builder()
					.platform(Platform.REDDIT)
					.title(title)
					.thumbnail(imgs.get(0))
					.url(imgs.get(0))
					.formats(List.of(MediaFormat.builder()
							.url(imgs.get(0))
							.mimeType(imgs.get(0).toLowerCase().endsWith(".gif") ? "image/gif" : "image/jpeg")
							.quality("Image")
							.build()))
					.build();
			log.extractionSuccess(Platform.REDDIT, url, 1, System.currentTimeMillis() - t0);
			return Extractions.ok(result);
		}
		return Extractions.fail("No downloadable video or image found in this post.");
	}

	private ExtractionResponse video(String videoUrl, String title, String url, long t0) {
		MediaResult result = MediaResult.builder()
				.platform(Platform.REDDIT)
				.title(title)
				.url(videoUrl)
				.formats(List.of(MediaFormat.builder()
						.url(videoUrl)
						.mimeType("video/mp4")
						.quality("HD")
						.build()))
				.build();
		log.extractionSuccess(Platform.REDDIT, url, 1, System.currentTimeMillis() - t0);
		return Extractions.ok(result);
	}

	/**
	 * From matches that may be either full {@code https://v.redd.it/<id>/DASH_<n>.mp4}
	 * URLs OR bare {@code DASH_<n>.mp4} names, keep the highest {@code n}. Full-URL
	 * matches win as-is; bare names have no base here and are ignored (handled by
	 * {@link #fetchManifestVideo}).
	 */
	private String highestDash(List<String> matches) {
		String bestUrl = null;
		int bestN = -1;
		for (String s : matches) {
			Matcher m = Pattern.compile("DASH_(\\d+)\\.mp4").matcher(s);
			if (m.find() && s.startsWith("http")) {
				int n = Integer.parseInt(m.group(1));
				if (n > bestN) {
					bestN = n;
					bestUrl = s;
				}
			}
		}
		return bestUrl;
	}

	private String titleOf(BrowserCapture capture) {
		try {
			var doc = Jsoup.parse(capture.getHtml() == null ? "" : capture.getHtml());
			String t = BrowserMedia.ogContent(doc, "og:title");
			if (t != null) return t;
			if (doc.title() != null && !doc.title().isBlank()) return doc.title();
		} catch (Exception ignore) {
		}
		return null;
	}
}