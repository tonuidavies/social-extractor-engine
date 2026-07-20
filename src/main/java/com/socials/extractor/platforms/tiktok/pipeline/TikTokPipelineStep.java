package com.socials.extractor.platforms.tiktok.pipeline;

import com.socials.extractor.pipeline.PipelineStep;

/**
 * Marker interface to ensure Spring Boot only injects 
 * TikTok-specific steps into the TikTokPipeline.
 */
public interface TikTokPipelineStep extends PipelineStep {
}