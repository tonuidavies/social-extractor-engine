package com.socials.extractor.platforms.meta.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MetaMetadata {

    private String mediaId;

    private String shortcode;

    private String title;

    private String description;

    private String thumbnail;

    private Long duration;

    private String username;

    private String displayName;

    private String profilePicture;

}