package com.socials.extractor.support.headers;

import com.socials.extractor.model.BrowserSession;
import org.springframework.http.HttpHeaders;

public interface HeaderBuilder {

    HttpHeaders build(
            BrowserSession session
    );

}