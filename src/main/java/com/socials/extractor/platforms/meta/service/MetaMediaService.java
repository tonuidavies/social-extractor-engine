package com.socials.extractor.platforms.meta.service;

import com.socials.extractor.browser.BrowserCapture;
import com.socials.extractor.model.MediaResult;
import com.socials.extractor.platforms.meta.resolver.browser.BrowserResolverChain;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MetaMediaService {

    private final BrowserResolverChain resolverChain;

    public MediaResult resolve(
            BrowserCapture capture
    ) {

        return resolverChain.resolve(
                capture
        );

    }

}