package com.socials.extractor.core.serverjs;

import lombok.Builder;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
public class ServerJsPayload {

    @Builder.Default
    private List<String> payloads = new ArrayList<>();

}