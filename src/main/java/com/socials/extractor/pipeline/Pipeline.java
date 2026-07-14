package com.socials.extractor.pipeline;

import reactor.core.publisher.Mono;

public interface Pipeline {

    Pipeline step(
            PipelineStep step
    );

    Mono<PipelineContext> execute(
            PipelineContext context
    );

}