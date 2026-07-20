package com.socials.extractor.platforms.meta.pipeline;

import com.socials.extractor.pipeline.PipelineStep;

/**
 * Marker interface to ensure Spring Boot only injects 
 * Meta-specific steps into the MetaPipeline.
 */
public interface MetaPipelineStep extends PipelineStep {
}