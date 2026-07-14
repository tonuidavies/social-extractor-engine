package com.socials.extractor.platforms.meta.pipeline.steps;

import com.fasterxml.jackson.databind.JsonNode;
import com.socials.extractor.core.serverjs.ServerJsExtractor;
import com.socials.extractor.core.serverjs.ServerJsParser;
import com.socials.extractor.core.serverjs.ServerJsPayload;
import com.socials.extractor.pipeline.PipelineContext;
import com.socials.extractor.pipeline.PipelineStep;
import com.socials.extractor.platforms.meta.parser.MediaDocumentBuilder;
import com.socials.extractor.platforms.meta.parser.MetaMediaLocator;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

import java.util.List;

//@Component
@RequiredArgsConstructor
public class ExtractSharedDataStep
        implements PipelineStep {

    private final ServerJsExtractor serverJsExtractor;

    private final ServerJsParser serverJsParser;

    private final MetaMediaLocator metaMediaLocator;

    private final MediaDocumentBuilder mediaDocumentBuilder;

    @Override
    public int order() {
        return 40;
    }

    @Override
    public Mono<PipelineContext> execute(
            PipelineContext context
    ) {

        try {

            ServerJsPayload payload =

                    serverJsExtractor.extract(
                            context.getDocument()
                    );

            System.out.println(
                    "ServerJS payloads : " +
                            payload.getPayloads().size()
            );

            List<JsonNode> nodes =

                    serverJsParser.parse(
                            payload
                    );

            System.out.println(
                    "Parsed JSON nodes : " +
                            nodes.size()
            );

            for (JsonNode node : nodes) {

                var media =

                        metaMediaLocator.locate(node);

                if (media.isPresent()) {

                    context.setMetaMediaDocument(

                            mediaDocumentBuilder.build(node)

                    );

                    System.out.println(
                            "Instagram media located."
                    );

                    return Mono.just(context);

                }

            }

            return Mono.error(

                    new IllegalStateException(
                            "Instagram media not found."
                    )

            );

        }

        catch (Exception e) {

            return Mono.error(e);

        }

    }

}