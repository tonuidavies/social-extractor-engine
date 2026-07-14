package com.socials.extractor.platforms.meta.pipeline;

import com.socials.extractor.model.ExtractionRequest;
import com.socials.extractor.model.ExtractionResponse;
import reactor.core.publisher.Mono;

public interface MetaPipeline {

    Mono<ExtractionResponse> execute(
            ExtractionRequest request
    );

}