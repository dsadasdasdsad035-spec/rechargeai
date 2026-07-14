package com.wildai.seo.web;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.ModelAndView;

@ControllerAdvice(assignableTypes = {
        PublicArticlePageController.class,
        PublicProductPageController.class
})
public class PublicPageExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(PublicPageExceptionHandler.class);

    @ExceptionHandler(PublicPageNotFoundException.class)
    public ModelAndView handleNotFound(
            PublicPageNotFoundException exception,
            HttpServletRequest request) {
        return errorPage(HttpStatus.NOT_FOUND, exception.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ModelAndView handleUnexpected(Exception exception, HttpServletRequest request) {
        log.error("公开页面处理失败，请求路径：{}", request.getRequestURI(), exception);
        return errorPage(HttpStatus.INTERNAL_SERVER_ERROR, "页面暂时不可用");
    }

    private ModelAndView errorPage(HttpStatus status, String message) {
        ModelAndView modelAndView = new ModelAndView("public/error");
        modelAndView.setStatus(status);
        modelAndView.addObject("statusCode", status.value());
        modelAndView.addObject("pageMessage", message);
        return modelAndView;
    }
}
