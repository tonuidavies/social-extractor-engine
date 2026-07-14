package com.socials.extractor.browser;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class MemoryCookieStore
        implements CookieStore {

    private final Map<String, String> cookies =
            new ConcurrentHashMap<>();

    @Override
    public void put(
            String name,
            String value
    ) {

        cookies.put(name, value);

    }

    @Override
    public String get(
            String name
    ) {

        return cookies.get(name);

    }

    @Override
    public Map<String, String> all() {

        return cookies;

    }

}