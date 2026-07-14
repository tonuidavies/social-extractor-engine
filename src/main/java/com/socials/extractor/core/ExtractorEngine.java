package com.socials.extractor.core;

import com.socials.extractor.model.ExtractionRequest;
import com.socials.extractor.model.ExtractionResponse;
import reactor.core.publisher.Mono;

public interface ExtractorEngine {

    Mono<ExtractionResponse> extract(
            ExtractionRequest request
    );

}