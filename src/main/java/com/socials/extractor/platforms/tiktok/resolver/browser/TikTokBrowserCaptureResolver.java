package com.socials.extractor.platforms.tiktok.resolver.browser;

import com.socials.extractor.browser.BrowserCapture;
import com.socials.extractor.model.MediaResult;

public interface TikTokBrowserCaptureResolver {
    boolean supports(BrowserCapture capture);
    MediaResult resolve(BrowserCapture capture);
}