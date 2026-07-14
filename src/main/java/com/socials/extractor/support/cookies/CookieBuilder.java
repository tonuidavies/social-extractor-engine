package com.socials.extractor.support.cookies;

import java.util.Map;

public interface CookieBuilder {

    String build(
            Map<String,String> cookies
    );

}