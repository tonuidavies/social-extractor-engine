package com.socials.extractor.browser.resolver;

import com.socials.extractor.browser.event.BrowserResponseEvent;

public class JsonBodyResolver {

    public boolean supports(
            BrowserResponseEvent event
    ) {

        return event

                .getContentType()

                .contains("application/json");

    }

}