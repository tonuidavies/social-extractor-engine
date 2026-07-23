package com.socials.extractor.api;

import com.socials.extractor.service.ScraperService;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/downloader")
@CrossOrigin(origins = "*")
public class VideoController {

    private final ScraperService scraperService;

    public VideoController(ScraperService scraperService) {
        this.scraperService = scraperService;
    }

    @PostMapping("/extract")
    public Mono<ResponseEntity<Map<String, String>>> extract(@RequestBody Map<String, String> request) {
        System.out.println("request: " + request);
        String url = request.get("url");
        if (url == null || url.isEmpty()) {
            return Mono.just(ResponseEntity.badRequest().body(Map.of("error", "URL is required")));
        }

        return Mono.fromCallable(() -> scraperService.getVideoInfo(url))
                .subscribeOn(Schedulers.boundedElastic())
                .map(ResponseEntity::ok)
                .onErrorResume(e -> Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("error", e.getMessage()))));
    }

    @GetMapping(value = "/stream", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public Flux<DataBuffer> streamVideo(@RequestParam("url") String videoUrl, ServerHttpResponse response) {
        // Set essential headers for React Native Blob Util / Axios compatibility
        response.getHeaders().add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"SaveItAll_Video.mp4\"");
        response.getHeaders().add(HttpHeaders.CONTENT_TYPE, "video/mp4");
        response.getHeaders().add(HttpHeaders.ACCEPT_RANGES, "bytes");

        return DataBufferUtils.readInputStream(
                () -> {
                    PipedOutputStream pos = new PipedOutputStream();
                    PipedInputStream pis = new PipedInputStream(pos);

                    // Offload blocking I/O to boundedElastic thread pool
                    Schedulers.boundedElastic().schedule(() -> {
                        try {
                            scraperService.streamVideoBytes(videoUrl, pos);
                        } catch (Exception e) {
                            // Log error; stream will naturally close
                        } finally {
                            try { pos.close(); } catch (IOException ignored) {}
                        }
                    });
                    return pis;
                },
                new org.springframework.core.io.buffer.DefaultDataBufferFactory(),
                8192
        ).subscribeOn(Schedulers.boundedElastic());
    }
}