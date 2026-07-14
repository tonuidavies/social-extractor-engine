package com.socials.extractor.platforms.meta.script;

import com.socials.extractor.browser.BrowserCapture;

import java.util.List;

public interface MetaScriptExtractor {

    List<String> extract(BrowserCapture capture);

}