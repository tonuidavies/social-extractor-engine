package com.socials.extractor.platforms.threads;

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
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;

/**
 * Threads (threads.net / threads.com) extractor via the shared Playwright browser.
 *
 * <p>Threads is a Meta property; its media is served from the Instagram/Facebook
 * CDNs (cdninstagram / fbcdn / scontent). We render the post, then take the
 * video from the Open Graph meta or a regex sweep of the page + network capture,
 * with an image fallback.
 */
@Component
@RequiredArgsConstructor
public class ThreadsExtractor implements Extractor {

	private static final AppLogger log = AppLogger.of(ThreadsExtractor.class);
	private static final String CHROME =
			"Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 "
					+ "(KHTML, like Gecko) Chrome/138.0.0.0 Safari/537.36";

	private final BrowserClient browserClient;

	@Override
	public boolean supports(String url) {
		if (url == null) return false;
		String u = url.toLowerCase();
		return u.contains("threads.net") || u.contains("threads.com");
	}

	@Override
	public Mono<ExtractionResponse> extract(ExtractionRequest request) {
		String url = request.getUrl();
		long t0 = System.currentTimeMillis();
		log.extractionStart(Platform.THREADS, url);

		return browserClient.capture(url, session())
				.map(capture -> build(capture, url, t0))
				.onErrorResume(e -> {
					log.extractionFailure(Platform.THREADS, url, e);
					return Mono.just(Extractions.fail("Couldn't load this Threads post."));
				});
	}

	private BrowserSession session() {
		return BrowserSession.builder()
				.userAgent(CHROME)
				.acceptLanguage("en-US,en;q=0.9")
				.acceptEncoding("identity")
				.referer("https://www.threads.net/")
				.build();
	}

	private ExtractionResponse build(BrowserCapture capture, String url, long t0) {
		String html = capture.getHtml() == null ? "" : capture.getHtml();
		String text = BrowserMedia.scanText(capture);
		Document doc = Jsoup.parse(html);

		List<MediaFormat> formats = new ArrayList<>();
		String title = BrowserMedia.ogContent(doc, "og:title", "twitter:title");
		String image = BrowserMedia.ogContent(doc, "og:image", "twitter:image");

		// 1) Open Graph video
		String og = BrowserMedia.ogContent(doc, "og:video", "og:video:url", "og:video:secure_url");
		if (og != null && og.contains(".mp4")) {
			formats.add(MediaFormat.builder().url(og).mimeType("video/mp4").quality("HD").build());
		}

		// 2) Regex sweep for a Meta-CDN .mp4 (Threads media = Instagram/FB CDNs)
		if (formats.isEmpty()) {
			String mp4 = BrowserMedia.firstMp4(text, "cdninstagram", "fbcdn", "scontent");
			if (mp4 != null) {
				formats.add(MediaFormat.builder().url(mp4).mimeType("video/mp4").quality("HD").build());
			}
		}

		// 3) Image fallback
		if (formats.isEmpty() && image != null) {
			formats.add(MediaFormat.builder().url(image).mimeType("image/jpeg").quality("Image").build());
		}

		if (formats.isEmpty()) {
			boolean login = html.toLowerCase().contains("log in") || html.length() < 2000;
			log.warn("[threads] no media (htmlLen={} login={}) url={}",
					html.length(), login, AppLogger.safeUrl(url));
			return Extractions.fail(login
					? "Threads required a login for this post, so it can't be saved."
					: "No downloadable media found in this post.");
		}

		MediaResult result = MediaResult.builder()
				.platform(Platform.THREADS)
				.title(title)
				.thumbnail(image)
				.url(formats.get(0).getUrl())
				.formats(formats)
				.build();

		log.extractionSuccess(Platform.THREADS, url, formats.size(), System.currentTimeMillis() - t0);
		return Extractions.ok(result);
	}
}