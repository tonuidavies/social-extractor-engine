package com.socials.extractor.platforms.tiktok.pipeline;

import com.socials.extractor.model.ExtractionRequest;
import com.socials.extractor.model.ExtractionResponse;
import reactor.core.publisher.Mono;

public interface TikTokPipeline {
    Mono<ExtractionResponse> execute(ExtractionRequest request);
}