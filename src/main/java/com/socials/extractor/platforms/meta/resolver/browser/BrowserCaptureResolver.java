package com.socials.extractor.platforms.meta.resolver.browser;

import com.socials.extractor.browser.BrowserCapture;
import com.socials.extractor.model.MediaResult;

public interface BrowserCaptureResolver {

    boolean supports(
            BrowserCapture capture
    );

    MediaResult resolve(
            BrowserCapture capture
    );

}