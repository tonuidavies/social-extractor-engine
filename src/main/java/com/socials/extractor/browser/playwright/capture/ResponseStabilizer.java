package com.socials.extractor.browser.playwright.capture;

import com.microsoft.playwright.Page;
import com.socials.extractor.browser.playwright.model.CaptureState;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ResponseStabilizer {

    private static final int STABILIZATION_MS = 150;

    private static final int EXTRA_WAIT_MS = 50;

    public void waitForStable(
            Page page,
            CaptureState state
    ) {

        int before =
                state.getResponses().size();

        page.waitForTimeout(
                STABILIZATION_MS
        );

        int after =
                state.getResponses().size();

        if (after > before) {

            page.waitForTimeout(
                    EXTRA_WAIT_MS
            );

        }

        log.debug(
                "Responses stabilized ({} -> {})",
                before,
                after
        );

    }

}