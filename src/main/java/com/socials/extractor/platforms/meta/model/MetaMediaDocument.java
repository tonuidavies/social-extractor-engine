package com.socials.extractor.platforms.meta.model;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MetaMediaDocument {

    /*
     * Entire payload
     */
    private JsonNode root;

    /*
     * Media node
     */
    private JsonNode media;

    /*
     * Metadata
     */
    private String mediaId;

    private String shortcode;

    private String ownerId;

    private String ownerUsername;

    private String caption;

    private String title;

    /*
     * Type
     */
    private boolean reel;

    private boolean story;

    private boolean post;

    private boolean carousel;

    private boolean video;

}