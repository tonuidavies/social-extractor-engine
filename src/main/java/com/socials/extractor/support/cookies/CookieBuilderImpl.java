package com.socials.extractor.support.cookies;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.stream.Collectors;

@Component
public class CookieBuilderImpl
        implements CookieBuilder {

    @Override
    public String build(
            Map<String,String> cookies
    ) {

        if (cookies == null || cookies.isEmpty()) {

            return "";

        }

        return cookies.entrySet()

                .stream()

                .map(e -> e.getKey() + "=" + e.getValue())

                .collect(Collectors.joining("; "));

    }

}