package com.socials.extractor.core.document;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DocumentArtifact {

    private ArtifactType type;

    private String content;

}