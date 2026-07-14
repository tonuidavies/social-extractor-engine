package com.socials.extractor.browser;

import java.util.Map;

public interface CookieStore {

    void put(
            String name,
            String value
    );

    String get(
            String name
    );

    Map<String, String> all();

}