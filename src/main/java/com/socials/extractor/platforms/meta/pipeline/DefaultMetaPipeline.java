package com.socials.extractor.platforms.meta.pipeline;

import com.socials.extractor.model.ExtractionRequest;
import com.socials.extractor.model.ExtractionResponse;
import com.socials.extractor.pipeline.PipelineContext;
import com.socials.extractor.pipeline.PipelineStep;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Comparator;
import java.util.List;

@Component
@RequiredArgsConstructor
public class DefaultMetaPipeline implements MetaPipeline {

    private final List<PipelineStep> steps;

    @Override
    public Mono<ExtractionResponse> execute(ExtractionRequest request) {
        PipelineContext context = PipelineContext.builder()
                .url(request.getUrl())
                .build();

        Mono<PipelineContext> pipeline = Mono.just(context);

        // Filter steps belonging strictly to the meta package
        List<PipelineStep> metaSteps = steps.stream()
                .filter(step -> step.getClass().getPackageName().contains("meta"))
                .sorted(Comparator.comparingInt(PipelineStep::order))
                .toList();

        for (PipelineStep step : metaSteps) {
            pipeline = pipeline.flatMap(step::execute);
        }

        return pipeline.map(this::response);
    }

    private ExtractionResponse response(PipelineContext context) {
        return ExtractionResponse.builder()
                .success(context.getResult() != null && context.getResult().getUrl() != null)
                .media(context.getResult())
                .build();
    }
}