package com.socials.extractor.platforms.meta.inspector;

import lombok.Builder;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
public class InspectionReport {

    @Builder.Default
    private List<String> scripts = new ArrayList<>();

    @Builder.Default
    private List<String> jsonScripts = new ArrayList<>();

    @Builder.Default
    private List<String> urls = new ArrayList<>();

    @Builder.Default
    private List<String> graphqlEndpoints = new ArrayList<>();

}