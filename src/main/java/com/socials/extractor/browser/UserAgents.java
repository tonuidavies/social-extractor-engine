package com.socials.extractor.browser;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public final class UserAgents {

    private UserAgents() {
    }

    private static final List<String> AGENTS = List.of(

            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/138.0.0.0 Safari/537.36",

            "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/138.0.0.0 Safari/537.36",

            "Mozilla/5.0 (Macintosh; Intel Mac OS X 14_5) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/138.0.0.0 Safari/537.36",

            "Mozilla/5.0 (Linux; Android 15; Pixel 9) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/138.0.0.0 Mobile Safari/537.36"

    );

    public static String random() {

        return AGENTS.get(

                ThreadLocalRandom.current()

                        .nextInt(AGENTS.size())

        );

    }

}