package com.wildai.seo.service;

import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicLong;

@Component
public class SitemapVersion {

    private final AtomicLong version = new AtomicLong();

    public long current() {
        return version.get();
    }

    public void invalidate() {
        version.incrementAndGet();
    }
}
