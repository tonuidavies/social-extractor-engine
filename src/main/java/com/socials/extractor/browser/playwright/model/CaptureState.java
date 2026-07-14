package com.socials.extractor.browser.playwright.model;

import com.socials.extractor.browser.BrowserRequest;
import com.socials.extractor.browser.BrowserResponse;
import lombok.Getter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Getter
public class CaptureState {

    private final List<BrowserRequest> requests =
            Collections.synchronizedList(
                    new ArrayList<>()
            );

    private final List<BrowserResponse> responses =
            Collections.synchronizedList(
                    new ArrayList<>()
            );

}