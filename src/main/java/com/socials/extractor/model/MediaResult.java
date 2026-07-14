package com.socials.extractor.model;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class MediaResult {

    private Platform platform;

    private String title;

    private String thumbnail;

    private String url;

    private Long duration;

    private List<MediaFormat> formats;

}