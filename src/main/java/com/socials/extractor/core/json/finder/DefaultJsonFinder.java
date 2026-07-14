package com.socials.extractor.core.json.finder;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class DefaultJsonFinder
        implements JsonFinder {

    @Override
    public List<String> find(
            String script
    ) {

        List<String> json =
                new ArrayList<>();

        int depth = 0;

        int start = -1;

        for (int i = 0; i < script.length(); i++) {

            char c = script.charAt(i);

            if (c == '{') {

                if (depth == 0) {

                    start = i;

                }

                depth++;

            }

            else if (c == '}') {

                depth--;

                if (depth == 0 && start >= 0) {

                    json.add(

                            script.substring(
                                    start,
                                    i + 1
                            )

                    );

                }

            }

        }

        return json;

    }

}