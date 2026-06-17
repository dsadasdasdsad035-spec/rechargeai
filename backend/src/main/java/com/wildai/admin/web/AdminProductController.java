package com.wildai.admin.web;

import com.wildai.common.dto.ApiResponse;
import com.wildai.product.domain.AiServiceProduct;
import com.wildai.product.dto.ProductDetailDto;
import com.wildai.product.repository.AiServiceProductRepository;
import com.wildai.product.service.ProductService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin/api/products")
public class AdminProductController {

    private final ProductService productService;
    private final AiServiceProductRepository productRepository;

    public AdminProductController(ProductService productService, AiServiceProductRepository productRepository) {
        this.productService = productService;
        this.productRepository = productRepository;
    }

    @GetMapping
    public ApiResponse<List<AiServiceProduct>> list() {
        return ApiResponse.ok(productService.listAll());
    }

    @PostMapping
    public ApiResponse<AiServiceProduct> create(@RequestBody AiServiceProduct product) {
        product.setId(null);
        return ApiResponse.ok(productService.save(product));
    }

    @PutMapping("/{id}")
    public ApiResponse<AiServiceProduct> update(@PathVariable Long id, @RequestBody AiServiceProduct product) {
        AiServiceProduct existing = productRepository.findById(id).orElseThrow();
        product.setId(existing.getId());
        return ApiResponse.ok(productService.save(product));
    }

    @PostMapping("/{id}/shelf")
    public ApiResponse<ProductDetailDto> shelf(@PathVariable Long id, @RequestBody Map<String, String> body) {
        AiServiceProduct p = productRepository.findById(id).orElseThrow();
        p.setStatus(body.get("status"));
        productService.save(p);
        return ApiResponse.ok(productService.getById(id));
    }
}
