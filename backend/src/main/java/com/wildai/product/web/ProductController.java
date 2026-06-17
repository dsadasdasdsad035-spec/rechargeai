package com.wildai.product.web;

import com.wildai.common.dto.ApiResponse;
import com.wildai.product.dto.ProductDetailDto;
import com.wildai.product.service.ProductService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    public ApiResponse<List<ProductDetailDto>> list() {
        return ApiResponse.ok(productService.listOnShelf());
    }

    @GetMapping("/{id}")
    public ApiResponse<ProductDetailDto> detail(@PathVariable Long id) {
        return ApiResponse.ok(productService.getById(id));
    }
}
