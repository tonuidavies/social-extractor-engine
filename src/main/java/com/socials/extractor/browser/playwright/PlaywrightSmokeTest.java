//package com.socials.extractor.browser.playwright;
//
//import com.socials.extractor.browser.BrowserCapture;
//import com.socials.extractor.browser.debug.BrowserCaptureInspector;
//import com.socials.extractor.model.BrowserSession;
//
//public class PlaywrightSmokeTest {
//
//    public static void main(String[] args) {
//
//        BrowserSession session =
//
//                BrowserSession.builder()
//
//                        .userAgent(
//                                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/138.0.0.0 Safari/537.36"
//                        )
//
//                        .build();
//
//        PlaywrightBrowserClient client =
//                new PlaywrightBrowserClient();//Expected 1 argument but found 0
//
//        BrowserCapture capture =
//
//                client
//
//                        .capture(
//                                "https://www.instagram.com/reel/DaikqoHgXM2/",
//                                session
//                        )
//
//                        .block();
//
//
//        new BrowserCaptureInspector()
//
//                .inspect(capture);
//    }
//
//}