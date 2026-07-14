package com.wildai.seo.web;

import com.wildai.common.exception.BusinessException;
import com.wildai.common.exception.ErrorCode;
import com.wildai.product.service.ProductService;
import com.wildai.seo.service.SeoMetadataFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class PublicProductPageController {

    private final ProductService productService;
    private final SeoMetadataFactory seoMetadataFactory;

    public PublicProductPageController(ProductService productService, SeoMetadataFactory seoMetadataFactory) {
        this.productService = productService;
        this.seoMetadataFactory = seoMetadataFactory;
    }

    @GetMapping("/")
    public ResponseEntity<Void> root() {
        return ResponseEntity.status(HttpStatus.MOVED_PERMANENTLY)
                .header(HttpHeaders.LOCATION, "/products")
                .build();
    }

    @GetMapping("/products")
    public String list(Model model) {
        var products = productService.listOnShelf();
        model.addAttribute("products", products);
        model.addAttribute("seo", seoMetadataFactory.forProductList(products));
        return "public/product-list";
    }

    @GetMapping("/products/{id}")
    public String detail(@PathVariable Long id, Model model) {
        try {
            var product = productService.getOnShelfById(id);
            model.addAttribute("product", product);
            model.addAttribute("seo", seoMetadataFactory.forProduct(product));
            return "public/product-detail";
        } catch (BusinessException exception) {
            if (exception.getErrorCode() == ErrorCode.NOT_FOUND
                    || exception.getErrorCode() == ErrorCode.PRODUCT_OFF_SHELF) {
                throw new PublicPageNotFoundException();
            }
            throw exception;
        }
    }
}
