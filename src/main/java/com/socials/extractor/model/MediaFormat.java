package com.socials.extractor.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MediaFormat {
    private  String id;
    private String type;
    private String mimeType;
    private String url;
    private String quality;
    private Integer width;
    private Integer height;
    private Long contentLength;
    private Long bitrate;

}