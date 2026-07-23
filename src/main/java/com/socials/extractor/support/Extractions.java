package com.socials.extractor.support;

import com.socials.extractor.model.ExtractionResponse;
import com.socials.extractor.model.MediaResult;

/**
 * Small shared helpers for building extractor responses so every platform
 * returns the same shape and quality labels.
 */
public final class Extractions {

	private Extractions() {}

	/** A realistic desktop UA for lightweight JSON/HTML fetches. */
	public static final String USER_AGENT =
			"Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
					+ "(KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36";

	public static ExtractionResponse ok(MediaResult media) {
		return ExtractionResponse.builder().success(true).media(media).build();
	}

	public static ExtractionResponse fail(String message) {
		return ExtractionResponse.builder().success(false).message(message).build();
	}

	/** Human quality label from a pixel height. */
	public static String quality(Integer height) {
		if (height == null || height <= 0) return "SD";
		if (height >= 2160) return "4K";
		if (height >= 1080) return "1080p";
		if (height >= 720) return "HD";
		return height + "p";
	}
}