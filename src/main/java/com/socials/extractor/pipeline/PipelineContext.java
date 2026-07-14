package com.socials.extractor.pipeline;

import com.socials.extractor.browser.BrowserCapture;
import com.socials.extractor.model.BrowserSession;
import com.socials.extractor.model.MediaResult;
import com.socials.extractor.platforms.meta.model.MetaMediaDocument;
import lombok.Builder;
import lombok.Data;
import org.jsoup.nodes.Document;

@Data
@Builder
public class PipelineContext {

    /*
     * Request
     */
    private String url;

    private BrowserSession browserSession;

    /*
     * Browser Capture (NEW)
     */
    private BrowserCapture browserCapture;

    /*
     * Legacy HTML pipeline
     */
    private String html;

    private Document document;

    /*
     * Instagram intermediate model
     */
    private MetaMediaDocument metaMediaDocument;

    /*
     * Final extraction result
     */
    private MediaResult result;

}