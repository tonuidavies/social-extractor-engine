package com.socials.extractor.support;

import com.socials.extractor.browser.BrowserCapture;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Shared helpers for browser-based extractors: read Open Graph meta, and scan
 * the rendered HTML + every captured request/response URL for media links.
 */
public final class BrowserMedia {

	private BrowserMedia() {}

	/** Any absolute .mp4 URL. */
	public static final Pattern MP4 =
			Pattern.compile("https?://[^\"'\\s\\\\)]+?\\.mp4[^\"'\\s\\\\)]*");

	/** First matching Open Graph / meta content value. */
	public static String ogContent(Document doc, String... properties) {
		for (String p : properties) {
			Element e = doc.selectFirst("meta[property=\"" + p + "\"]");
			if (e == null) e = doc.selectFirst("meta[name=\"" + p + "\"]");
			if (e != null) {
				String c = e.attr("content");
				if (c != null && !c.isBlank()) return c.replace("&amp;", "&");
			}
		}
		return null;
	}

	/** HTML + all captured request/response URLs, concatenated for regex scanning. */
	public static String scanText(BrowserCapture capture) {
		StringBuilder sb = new StringBuilder(capture.getHtml() == null ? "" : capture.getHtml());
		if (capture.getRequests() != null) {
			capture.getRequests().forEach(r -> sb.append('\n').append(r.getUrl()));
		}
		if (capture.getResponses() != null) {
			capture.getResponses().forEach(r -> sb.append('\n').append(r.getUrl()));
		}
		return sb.toString();
	}

	/** Unique regex matches, preserving order. */
	public static List<String> matches(String text, Pattern pattern) {
		Set<String> out = new LinkedHashSet<>();
		Matcher m = pattern.matcher(text);
		while (m.find()) out.add(m.group());
		return new ArrayList<>(out);
	}

	/** First .mp4 whose host contains any of the given fragments. */
	public static String firstMp4(String text, String... hostContains) {
		for (String u : matches(text, MP4)) {
			String low = u.toLowerCase();
			for (String frag : hostContains) {
				if (low.contains(frag)) return u;
			}
		}
		return null;
	}
}