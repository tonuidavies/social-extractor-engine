package com.socials.extractor.core;

import com.socials.extractor.model.ExtractionRequest;
import com.socials.extractor.model.ExtractionResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class ExtractorEngineImpl implements ExtractorEngine {

    private final ExtractorRegistry registry;

    @Override
    public Mono<ExtractionResponse> extract(ExtractionRequest request) {
        return registry
                .find(request.getUrl())
                .extract(request);

    }

}