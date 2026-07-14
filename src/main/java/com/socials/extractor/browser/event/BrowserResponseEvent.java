package com.socials.extractor.browser.event;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

import java.util.Map;

@Data
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class BrowserResponseEvent
        extends BrowserEvent {

    private int status;

    private String contentType;

    private Map<String,String> headers;

    private byte[] body;

}