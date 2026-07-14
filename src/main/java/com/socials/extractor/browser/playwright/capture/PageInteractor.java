package com.socials.extractor.browser.playwright.capture;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.PlaywrightException;
import com.microsoft.playwright.options.WaitUntilState;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;

@Slf4j
@Component
public class PageInteractor {

    private static final int VIDEO_WAIT_MS = 1_000;

    public void open(
            Page page,
            String url
    ) {

        page.navigate(

                url,

                new Page.NavigateOptions()

                        .setWaitUntil(
                                WaitUntilState.DOMCONTENTLOADED
                        )

        );

        log.info("Navigation finished.");
        log.info("Current URL: {}", page.url());

        try {

            String title = page.title();

            log.info("Page title: {}", title);

        }

        catch (Exception ex) {

            log.warn("Unable to read page title.", ex);

        }

        try {

            String html = page.content();

            log.info("HTML length: {}", html.length());

            Files.writeString(

                    Path.of("/tmp/instagram.html"),

                    html

            );

            log.info("Saved page HTML to /tmp/instagram.html");

        }

        catch (Exception ex) {

            log.warn("Unable to save page HTML.", ex);

        }

        /*
         * Give the page a brief moment to attach scripts.
         */
        page.waitForTimeout(300);

        /*
         * Strategy 1:
         * Click the video element if one exists.
         */
        try {

            Locator videos = page.locator("video");

            int count = videos.count();

            log.info("Video elements found: {}", count);

            if (count > 0) {

                Locator video = videos.first();

                video.scrollIntoViewIfNeeded();

                video.click(

                        new Locator.ClickOptions()

                                .setForce(true)

                                .setTimeout(VIDEO_WAIT_MS)

                );

                log.info("Clicked <video> element.");

                return;

            }

        }

        catch (PlaywrightException ex) {

            log.debug("Unable to click video element.", ex);

        }

        /*
         * Strategy 2:
         * Click the center of the viewport.
         */
        try {

            page.mouse().click(

                    200,

                    350

            );

            log.info("Clicked page center.");

            page.waitForTimeout(200);

        }

        catch (Exception ex) {

            log.debug("Center click failed.", ex);

        }

        /*
         * Strategy 3:
         * Small scroll to trigger lazy loading.
         */
        try {

            page.mouse().wheel(

                    0,

                    400

            );

            log.info("Scrolled page.");

            page.waitForTimeout(200);

        }

        catch (Exception ex) {

            log.debug("Scroll failed.", ex);

        }

        /*
         * Strategy 4:
         * Click again after scrolling.
         */
        try {

            page.mouse().click(

                    200,

                    350

            );

            log.info("Clicked page center again.");

        }

        catch (Exception ex) {

            log.debug("Second click failed.", ex);

        }

    }

}