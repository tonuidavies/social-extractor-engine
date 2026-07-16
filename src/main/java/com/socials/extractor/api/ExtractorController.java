package com.socials.extractor.api;

import com.socials.extractor.core.ExtractorEngine;
import com.socials.extractor.model.ExtractionRequest;
import com.socials.extractor.model.ExtractionResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v1/extract")
@RequiredArgsConstructor
@CrossOrigin("*")
public class ExtractorController {

    private final ExtractorEngine extractorEngine;

    @PostMapping
    public Mono<ExtractionResponse> extract(@Valid @RequestBody ExtractionRequest request) {

        return extractorEngine.extract(request);

    }

}