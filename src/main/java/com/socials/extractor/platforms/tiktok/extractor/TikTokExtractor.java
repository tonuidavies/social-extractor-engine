package com.socials.extractor.platforms.tiktok.extractor;

import com.socials.extractor.core.Extractor;
import com.socials.extractor.model.ExtractionRequest;
import com.socials.extractor.model.ExtractionResponse;
import com.socials.extractor.platforms.tiktok.pipeline.TikTokPipeline;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class TikTokExtractor implements Extractor {

    private final TikTokPipeline pipeline;

    @Override
    public boolean supports(String url) {
        if (url == null) return false;
        return url.contains("tiktok.com");
    }

    @Override
    public Mono<ExtractionResponse> extract(ExtractionRequest request) {
        return pipeline.execute(request);
    }
}