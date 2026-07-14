package com.socials.extractor.pipeline;

import reactor.core.publisher.Mono;

public interface PipelineStep {

    int order();

    Mono<PipelineContext> execute(
            PipelineContext context
    );

}