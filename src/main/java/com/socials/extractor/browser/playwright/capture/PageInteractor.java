package com.socials.extractor.browser.playwright.capture;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.WaitUntilState;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class PageInteractor {

    public void open(Page page, String url) {
        page.navigate(url, new Page.NavigateOptions().setWaitUntil(WaitUntilState.DOMCONTENTLOADED));
        page.waitForTimeout(500);

        try {
            Locator videos = page.locator("video");
            if (videos.count() > 0) {
                videos.first().scrollIntoViewIfNeeded();
                videos.first().click(new com.microsoft.playwright.Locator.ClickOptions().setForce(true));
            } else {
                page.mouse().click(200, 350);
                page.mouse().wheel(0, 400);
            }
        } catch (Exception ex) {
            log.debug("Interaction sequence completed with minor issues: {}", ex.getMessage());
        }
    }
}