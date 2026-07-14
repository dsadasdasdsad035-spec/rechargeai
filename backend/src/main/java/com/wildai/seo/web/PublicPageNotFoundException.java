package com.wildai.seo.web;

public class PublicPageNotFoundException extends RuntimeException {

    public PublicPageNotFoundException() {
        super("页面不存在");
    }
}
