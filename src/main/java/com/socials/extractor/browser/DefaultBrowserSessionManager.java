package com.socials.extractor.browser;

import com.socials.extractor.model.BrowserSession;
import org.springframework.stereotype.Component;

@Component
public class DefaultBrowserSessionManager
        implements BrowserSessionManager {

    private static final String CHROME_138 =

            "Mozilla/5.0 (X11; Linux x86_64) " +
                    "AppleWebKit/537.36 (KHTML, like Gecko) " +
                    "Chrome/138.0.0.0 Safari/537.36";

    @Override
    public BrowserSession create() {

        return BrowserSession.builder()

                .userAgent(CHROME_138)

                .acceptLanguage("en-US,en;q=0.9")

                .acceptEncoding("identity")

                .referer("https://www.instagram.com/")

                .build();

    }

}