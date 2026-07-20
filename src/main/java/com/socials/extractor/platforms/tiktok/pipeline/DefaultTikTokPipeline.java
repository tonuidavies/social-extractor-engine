package com.socials.extractor.platforms.tiktok.pipeline;

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
public class DefaultTikTokPipeline implements TikTokPipeline {

    private final List<PipelineStep> steps;

    @Override
    public Mono<ExtractionResponse> execute(ExtractionRequest request) {
        PipelineContext context = PipelineContext.builder()
                .url(request.getUrl())
                .build();

        Mono<PipelineContext> pipeline = Mono.just(context);

        // Filter steps belonging strictly to the tiktok package
        List<PipelineStep> tiktokSteps = steps.stream()
                .filter(step -> step.getClass().getPackageName().contains("tiktok"))
                .sorted(Comparator.comparingInt(PipelineStep::order))
                .toList();

        for (PipelineStep step : tiktokSteps) {
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