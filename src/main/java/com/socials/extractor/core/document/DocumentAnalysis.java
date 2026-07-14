package com.socials.extractor.core.document;

import lombok.Builder;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
public class DocumentAnalysis {

    @Builder.Default
    private List<DocumentArtifact> artifacts =
            new ArrayList<>();

}