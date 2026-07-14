package com.socials.extractor.browser.debug;

import com.socials.extractor.browser.BrowserCapture;
import com.socials.extractor.browser.BrowserResponse;

public class JsonDiscovery {

    public void inspect(
            BrowserCapture capture
    ) {

        for (BrowserResponse response :

                capture.jsonResponses()) {

            System.out.println();
            System.out.println("===============================");
            System.out.println(response.getUrl());
            System.out.println("===============================");

            System.out.println(

                    new String(response.getBody())

                            .substring(

                                    0,

                                    Math.min(

                                            500,

                                            response.getBody().length

                                    )

                            )

            );

        }

    }

}