package com.socials.extractor.platforms.tiktok.service;

import com.socials.extractor.browser.BrowserCapture;
import com.socials.extractor.model.MediaResult;
import com.socials.extractor.platforms.tiktok.resolver.browser.TikTokBrowserResolverChain;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TikTokMediaService {
    private final TikTokBrowserResolverChain resolverChain;

    public MediaResult resolve(BrowserCapture capture) {
        return resolverChain.resolve(capture);
    }
}