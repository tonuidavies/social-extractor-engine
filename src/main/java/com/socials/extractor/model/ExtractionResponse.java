package com.socials.extractor.model;

import com.socials.extractor.browser.BrowserCapture;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExtractionResponse {

    private boolean success;

    private String message;

    private MediaResult media;

    private long executionTime;
//    private BrowserCapture capture;

}