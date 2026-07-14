package com.socials.extractor.browser.event;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

import java.util.Map;

@Data
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class BrowserRequestEvent
        extends BrowserEvent {

    private String method;

    private String resourceType;

    private Map<String,String> headers;

    private String postData;

}