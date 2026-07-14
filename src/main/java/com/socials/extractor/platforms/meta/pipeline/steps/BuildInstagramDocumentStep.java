package com.socials.extractor.platforms.meta.pipeline.steps;

import com.socials.extractor.pipeline.PipelineContext;
import com.socials.extractor.pipeline.PipelineStep;
import com.socials.extractor.platforms.meta.parser.MetaDocumentParser;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;
import org.jsoup.nodes.Element;
import java.nio.file.Files;
import java.nio.file.Path;

//@Component
@RequiredArgsConstructor
public class BuildInstagramDocumentStep
        implements PipelineStep {

    private final MetaDocumentParser parser;

    @Override
    public int order() {
        return 40;
    }

    @Override
    public Mono<PipelineContext> execute(
            PipelineContext context
    ) {

        int index = 0;

        for (Element script : context.getDocument().select("script")) {

            String html = script.html();

            if (html == null || html.isBlank()) {
                continue;
            }

            if (!html.contains("ScheduledServerJS")) {
                continue;
            }

            System.out.println();
            System.out.println("========================================");
            System.out.println("ScheduledServerJS #" + index++);
            System.out.println("Length : " + html.length());

            try {

                Files.writeString(

                        Path.of(
                                "/tmp/serverjs-" + index + ".js"
                        ),

                        html

                );

            }
            catch (Exception e) {

                e.printStackTrace();

            }

            System.out.println(
                    "Saved to /tmp/serverjs-" + index + ".js"
            );

        }

        return Mono.error(

                new IllegalStateException(
                        "Inspection complete."
                )

        );

    }
}