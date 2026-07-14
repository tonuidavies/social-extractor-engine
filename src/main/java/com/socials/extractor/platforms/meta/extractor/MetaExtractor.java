package com.socials.extractor.platforms.meta.extractor;

import com.socials.extractor.core.Extractor;
import com.socials.extractor.model.ExtractionRequest;
import com.socials.extractor.model.ExtractionResponse;
import com.socials.extractor.platforms.meta.pipeline.MetaPipeline;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
@Component
@RequiredArgsConstructor
public class MetaExtractor
        implements Extractor {

    private final MetaPipeline pipeline;

    @Override
    public boolean supports(
            String url
    ) {

        if (url == null) {
            return false;
        }

        return url.contains("instagram.com")
                || url.contains("facebook.com")
                || url.contains("fb.watch");
    }

    @Override
    public Mono<ExtractionResponse> extract(
            ExtractionRequest request
    ) {

        return pipeline.execute(request);

    }

}