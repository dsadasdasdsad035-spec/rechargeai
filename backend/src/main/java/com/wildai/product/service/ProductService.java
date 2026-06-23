package com.wildai.product.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wildai.common.exception.BusinessException;
import com.wildai.common.exception.ErrorCode;
import com.wildai.product.domain.AiServiceProduct;
import com.wildai.product.dto.ProductDetailDto;
import com.wildai.product.repository.AiServiceProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class ProductService {

    private static final String DEFAULT_REQUIRED_FIELDS_JSON =
            "[{\"key\":\"target_account\",\"label\":\"AI 账号邮箱\",\"type\":\"email\",\"required\":true}]";

    private final AiServiceProductRepository productRepo;
    private final ObjectMapper objectMapper;

    public ProductService(AiServiceProductRepository productRepo, ObjectMapper objectMapper) {
        this.productRepo = productRepo;
        this.objectMapper = objectMapper;
    }

    public List<ProductDetailDto> listOnShelf() {
        return productRepo.findByStatusOrderBySortOrderAsc("ON_SHELF").stream().map(this::toDto).toList();
    }

    public ProductDetailDto getById(Long id) {
        AiServiceProduct p = productRepo.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "产品不存在"));
        return toDto(p);
    }

    public AiServiceProduct requireOnShelf(Long id) {
        AiServiceProduct p = productRepo.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "产品不存在"));
        if (!"ON_SHELF".equals(p.getStatus())) {
            throw new BusinessException(ErrorCode.PRODUCT_OFF_SHELF);
        }
        return p;
    }

    public void validateOrderFields(AiServiceProduct product, Map<String, String> fields) {
        try {
            JsonNode arr = objectMapper.readTree(product.getRequiredFieldsJson());
            for (JsonNode node : arr) {
                String key = node.get("key").asText();
                boolean required = node.path("required").asBoolean(true);
                String val = fields.get(key);
                if (required && (val == null || val.isBlank())) {
                    throw new BusinessException(ErrorCode.BAD_REQUEST, "请填写" + node.path("label").asText());
                }
                if ("password".equalsIgnoreCase(key)) {
                    throw new BusinessException(ErrorCode.BAD_REQUEST, "不允许提交第三方密码");
                }
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "产品字段配置错误");
        }
    }

    @Transactional
    public AiServiceProduct create(AiServiceProduct product) {
        if (product.getName() == null || product.getName().isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "请填写产品名称");
        }
        if (product.getSalePrice() == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "请填写售价");
        }
        product.setId(null);
        if (product.getProductCode() == null || product.getProductCode().isBlank()) {
            product.setProductCode(generateUniqueProductCode(product.getName()));
        } else if (productRepo.existsByProductCode(product.getProductCode())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "产品编码已存在");
        }
        if (product.getServiceType() == null || product.getServiceType().isBlank()) {
            product.setServiceType("GENERAL");
        }
        if (product.getRequiredFieldsJson() == null || product.getRequiredFieldsJson().isBlank()) {
            product.setRequiredFieldsJson(DEFAULT_REQUIRED_FIELDS_JSON);
        }
        if (product.getStatus() == null || product.getStatus().isBlank()) {
            product.setStatus("OFF_SHELF");
        }
        if (product.getCurrency() == null || product.getCurrency().isBlank()) {
            product.setCurrency("CNY");
        }
        if (product.getPeriodDays() == null) {
            product.setPeriodDays(30);
        }
        if (product.getSortOrder() == null) {
            product.setSortOrder((int) productRepo.count());
        }
        Instant now = Instant.now();
        product.setCreatedAt(now);
        product.setUpdatedAt(now);
        return productRepo.save(product);
    }

    @Transactional
    public AiServiceProduct save(AiServiceProduct product) {
        product.setUpdatedAt(Instant.now());
        return productRepo.save(product);
    }

    private String generateUniqueProductCode(String name) {
        String base = name.toUpperCase()
                .replaceAll("[^A-Z0-9\\u4e00-\\u9fa5]", "_")
                .replaceAll("_+", "_")
                .replaceAll("^_|_$", "");
        if (base.isBlank()) {
            base = "PRODUCT";
        }
        if (base.length() > 48) {
            base = base.substring(0, 48);
        }
        String code = base;
        int suffix = 1;
        while (productRepo.existsByProductCode(code)) {
            code = base + "_" + suffix++;
        }
        if (code.length() > 64) {
            code = ("P_" + UUID.randomUUID().toString().replace("-", "")).substring(0, 64);
        }
        return code;
    }

    public List<AiServiceProduct> listAll() {
        return productRepo.findAllByOrderBySortOrderAsc();
    }

    private ProductDetailDto toDto(AiServiceProduct p) {
        return new ProductDetailDto(p.getId(), p.getProductCode(), p.getName(), p.getServiceType(),
                p.getOfficialPrice(), p.getSalePrice(), p.getCurrency(), p.getPeriodDays(), p.getStatus(),
                p.getRequiredFieldsJson(), p.getEstimatedHours(), p.getRefundPolicyText(), p.getComplianceNotice());
    }
}
