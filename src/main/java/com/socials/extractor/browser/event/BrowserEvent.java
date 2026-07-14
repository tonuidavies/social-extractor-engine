package com.socials.extractor.browser.event;

import lombok.Data;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
public abstract class BrowserEvent {

    private long timestamp;

    private String url;

}