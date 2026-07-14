package com.socials.extractor.platforms.meta.pipeline.steps;

import com.socials.extractor.core.parser.HtmlParser;
import com.socials.extractor.pipeline.PipelineContext;
import com.socials.extractor.pipeline.PipelineStep;
import lombok.RequiredArgsConstructor;
import org.jsoup.nodes.Document;
import reactor.core.publisher.Mono;

//@Component
@RequiredArgsConstructor
public class ParseHtmlStep
        implements PipelineStep {

    private final HtmlParser parser;

    @Override
    public int order() {
        return 30;
    }

    @Override
    public Mono<PipelineContext> execute(
            PipelineContext context
    ) {

        String html = context.getHtml();

        if (html == null) {

            return Mono.error(
                    new IllegalStateException("HTML is null.")
            );

        }

        System.out.println("========== PARSE ==========");
        System.out.println("HTML length : " + html.length());

        System.out.println(
                html.substring(
                        0,
                        Math.min(500, html.length())
                )
        );

        Document document =
                parser.parse(html);

        System.out.println(
                "Title : " + document.title()
        );

        System.out.println(
                "Scripts : " +
                        document.select("script").size()
        );

        System.out.println("===========================");

        context.setDocument(document);

        return Mono.just(context);

    }

}