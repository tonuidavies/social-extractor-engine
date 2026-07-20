package com.socials.extractor.pipeline;

import com.socials.extractor.browser.BrowserCapture;
import com.socials.extractor.model.BrowserSession;
import com.socials.extractor.model.MediaResult;
import com.socials.extractor.platforms.meta.model.MetaMediaDocument;
import lombok.Builder;
import lombok.Data;
import org.jsoup.nodes.Document;

import java.util.HashMap;
import java.util.Map;

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


//package com.socials.extractor.pipeline;
//
//import com.socials.extractor.browser.BrowserCapture;
//import com.socials.extractor.model.BrowserSession;
//import com.socials.extractor.model.MediaResult;
//import com.socials.extractor.platforms.meta.model.MetaMediaDocument;
//import lombok.Builder;
//import lombok.Data;
//import org.jsoup.nodes.Document;
//
//import java.util.HashMap;
//import java.util.Map;
//
//@Data
//@Builder
//public class PipelineContext {
//
//    /*
//     * Request
//     */
//    private String url;
//
//    private BrowserSession browserSession;
//
//    /*
//     * Browser Capture
//     */
//    private BrowserCapture browserCapture;
//
//    /*
//     * Legacy HTML pipeline
//     */
//    private String html;
//
//    private Document document;
//
//    /*
//     * Platform intermediate models
//     */
//    private MetaMediaDocument metaMediaDocument;
//
//    @Builder.Default
//    private Map<String, Object> attributes = new HashMap<>();
//
//    /*
//     * Final extraction result
//     */
//    private MediaResult result;
//
//    // Helper methods for clean platform attribute storage
//    public void setAttribute(String key, Object value) {
//        if (attributes == null) attributes = new HashMap<>();
//        attributes.put(key, value);
//    }
//
//    public Object getAttribute(String key) {
//        return attributes != null ? attributes.get(key) : null;
//    }
//}