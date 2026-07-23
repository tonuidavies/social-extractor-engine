package com.socials.extractor.api.advice;

import com.socials.extractor.model.ExtractionResponse;
import com.socials.extractor.support.logging.AppLogger;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.server.ResponseStatusException;

import java.util.concurrent.TimeoutException;

/**
 * Central error handling for the whole API.
 *
 * <p>Every failure is turned into the SAME {@link ExtractionResponse} shape the
 * mobile app already understands ({@code success:false}, {@code message}, no
 * {@code media}), with an appropriate HTTP status. That means the client's
 * existing error path ("if !ok or !success → show message") works for any error
 * without special-casing, and stack traces never leak to the client.
 *
 * <p>Works for WebFlux annotated controllers: exceptions raised inside the
 * reactive chain are routed here just like synchronous ones.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

	private static final AppLogger log = AppLogger.of(GlobalExceptionHandler.class);

	/** @Valid request body failed (e.g. blank url). */
	@ExceptionHandler(WebExchangeBindException.class)
	public ResponseEntity<ExtractionResponse> handleValidation(WebExchangeBindException ex) {
		String message = ex.getFieldErrors().stream()
				.findFirst()
				.map(f -> {
					String m = f.getDefaultMessage();
					return (f.getField() + " " + (m == null ? "is invalid" : m)).trim();
				})
				.orElse("Invalid request.");
		log.warn("[400] validation: {}", message);
		return body(HttpStatus.BAD_REQUEST, message);
	}

	/**
	 * Thrown by ExtractorRegistry.find(...) for an unsupported platform, and by
	 * any resolver that rejects malformed input.
	 */
	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<ExtractionResponse> handleIllegalArgument(IllegalArgumentException ex) {
		String message = ex.getMessage() == null
				? "This link isn't supported."
				: ex.getMessage();
		log.warn("[422] bad request: {}", message);
		return body(HttpStatus.UNPROCESSABLE_ENTITY, message);
	}

	/** Upstream (browser capture / CDN) took too long. */
	@ExceptionHandler(TimeoutException.class)
	public ResponseEntity<ExtractionResponse> handleTimeout(TimeoutException ex) {
		log.warn("[504] upstream timeout: {}", ex.getMessage());
		return body(
				HttpStatus.GATEWAY_TIMEOUT,
				"The platform took too long to respond. Please try again.");
	}

	/** A resolver deliberately signalled an HTTP status (e.g. blocked / not found). */
	@ExceptionHandler(ResponseStatusException.class)
	public ResponseEntity<ExtractionResponse> handleResponseStatus(ResponseStatusException ex) {
		HttpStatus status = HttpStatus.resolve(ex.getStatusCode().value());
		if (status == null) status = HttpStatus.BAD_GATEWAY;
		String message = ex.getReason() != null ? ex.getReason() : "The platform rejected the request.";
		log.warn("[{}] {}", status.value(), message);
		return body(status, message);
	}

	/** Anything else — log the full stack, tell the client something generic. */
	@ExceptionHandler(Throwable.class)
	public ResponseEntity<ExtractionResponse> handleUnexpected(Throwable ex) {
		log.error("[500] unhandled error during extraction", ex);
		return body(
				HttpStatus.INTERNAL_SERVER_ERROR,
				"Something went wrong while extracting media. Please try again.");
	}

	private ResponseEntity<ExtractionResponse> body(HttpStatus status, String message) {
		return ResponseEntity.status(status).body(
				ExtractionResponse.builder()
						.success(false)
						.message(message)
						.media(null)
						.build());
	}
}
