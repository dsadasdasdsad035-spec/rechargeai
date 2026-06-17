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

import java.util.List;
import java.util.Map;

@Service
public class ProductService {

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
    public AiServiceProduct save(AiServiceProduct product) {
        return productRepo.save(product);
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
