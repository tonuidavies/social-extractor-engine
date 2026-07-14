package com.socials.extractor.support.headers;

import com.socials.extractor.model.BrowserSession;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

@Component
public class HeaderBuilderImpl
        implements HeaderBuilder {

    @Override
    public HttpHeaders build(
            BrowserSession session
    ) {

        HttpHeaders headers =
                new HttpHeaders();

        if (session == null) {
            return headers;
        }

        if (session.getUserAgent() != null) {

            headers.set(
                    HttpHeaders.USER_AGENT,
                    session.getUserAgent()
            );

        }

        if (session.getReferer() != null) {

            headers.set(
                    HttpHeaders.REFERER,
                    session.getReferer()
            );

        }

        if (session.getAcceptLanguage() != null) {

            headers.set(
                    HttpHeaders.ACCEPT_LANGUAGE,
                    session.getAcceptLanguage()
            );

        }

        if (session.getAcceptEncoding() != null) {

            headers.set(
                    HttpHeaders.ACCEPT_ENCODING,
                    session.getAcceptEncoding()
            );

        }

        headers.set(
                HttpHeaders.ACCEPT,
                "*/*"
        );

        /*
         * Add any custom headers stored in the session.
         */
        if (session.getHeaders() != null) {

            session.getHeaders()

                    .forEach(headers::set);

        }

        return headers;

    }

}