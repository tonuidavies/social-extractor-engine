package com.socials.extractor.browser.playwright.capture;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.WaitUntilState;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Opens the page and nudges media to load.
 *
 * <p>Only change from the original: the video click is bounded to 2.5s (was
 * Playwright's default 30s), so on sites where clicking the video triggers
 * navigation (e.g. Reddit) the capture can't hang. Uses only the widely-available
 * ClickOptions.setTimeout to avoid any API-version issues.
 */
@Slf4j
@Component
public class PageInteractor {

    public void open(Page page, String url) {
        page.navigate(url, new Page.NavigateOptions()
                .setWaitUntil(WaitUntilState.DOMCONTENTLOADED));
        page.waitForTimeout(600);

        try {
            Locator videos = page.locator("video");
            if (videos.count() > 0) {
                videos.first().click(
                        new Locator.ClickOptions().setForce(true).setTimeout(2500));
            } else {
                page.mouse().click(200, 350);
                page.mouse().wheel(0, 400);
            }
        } catch (Exception ex) {
            log.debug("Interaction sequence completed with minor issues: {}", ex.getMessage());
        }
    }
}