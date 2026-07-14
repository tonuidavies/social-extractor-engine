package com.socials.extractor.core;

import com.socials.extractor.model.ExtractionRequest;
import com.socials.extractor.model.ExtractionResponse;
import reactor.core.publisher.Mono;

public interface Extractor {

    boolean supports(
            String url
    );

    Mono<ExtractionResponse> extract(
            ExtractionRequest request
    );

}