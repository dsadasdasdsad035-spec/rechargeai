package com.wildai.product.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wildai.common.exception.BusinessException;
import com.wildai.product.domain.AiServiceProduct;
import com.wildai.product.repository.AiServiceProductRepository;
import com.wildai.seo.service.SitemapVersion;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProductServiceTest {

    private final AiServiceProductRepository productRepo = mock(AiServiceProductRepository.class);
    private final ServiceTypeConfigService serviceTypeConfigService = mock(ServiceTypeConfigService.class);
    private final SitemapVersion sitemapVersion = mock(SitemapVersion.class);
    private final ProductService productService = new ProductService(
            productRepo,
            new ObjectMapper(),
            serviceTypeConfigService,
            sitemapVersion);

    @Test
    void createRejectsNonPositiveSalePrice() {
        for (String salePrice : new String[]{"0", "-0.01"}) {
            var product = product(salePrice, "CNY", "ON_SHELF");

            assertThatThrownBy(() -> productService.create(product))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("售价必须大于 0");
        }

        verify(productRepo, never()).save(any());
    }

    @Test
    void saveRejectsNonPositiveSalePrice() {
        for (String salePrice : new String[]{"0", "-0.01"}) {
            var product = product(salePrice, "CNY", "OFF_SHELF");

            assertThatThrownBy(() -> productService.save(product))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("售价必须大于 0");
        }

        verify(productRepo, never()).save(any());
    }

    @Test
    void createRejectsInvalidCurrencyAndStatus() {
        assertThatThrownBy(() -> productService.create(product("9.90", "INVALID", "ON_SHELF")))
                .isInstanceOf(BusinessException.class)
                .hasMessage("币种必须为 ISO 4217 三字母代码");
        assertThatThrownBy(() -> productService.create(product("9.90", "CNY", "UNKNOWN")))
                .isInstanceOf(BusinessException.class)
                .hasMessage("产品状态无效");

        verify(productRepo, never()).save(any());
    }

    @Test
    void saveRejectsInvalidCurrencyAndStatus() {
        assertThatThrownBy(() -> productService.save(product("9.90", "INVALID", "ON_SHELF")))
                .isInstanceOf(BusinessException.class)
                .hasMessage("币种必须为 ISO 4217 三字母代码");
        assertThatThrownBy(() -> productService.save(product("9.90", "CNY", "UNKNOWN")))
                .isInstanceOf(BusinessException.class)
                .hasMessage("产品状态无效");

        verify(productRepo, never()).save(any());
    }

    @Test
    void createNormalizesLowercaseUsdBeforeSaving() {
        var product = product("9.90", " usd ", "ON_SHELF");
        when(productRepo.save(any(AiServiceProduct.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var saved = productService.create(product);

        assertThat(saved.getCurrency()).isEqualTo("USD");
        verify(productRepo).save(product);
    }

    @Test
    void saveNormalizesLowercaseUsdBeforeSaving() {
        var product = product("9.90", "usd", "OFF_SHELF");
        when(productRepo.save(any(AiServiceProduct.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var saved = productService.save(product);

        assertThat(saved.getCurrency()).isEqualTo("USD");
        verify(productRepo).save(product);
    }

    @Test
    void saveAppliesCurrencyAndStatusDefaultsBeforeValidation() {
        var product = product("9.90", " ", " ");
        when(productRepo.save(any(AiServiceProduct.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var saved = productService.save(product);

        assertThat(saved.getCurrency()).isEqualTo("CNY");
        assertThat(saved.getStatus()).isEqualTo("OFF_SHELF");
    }

    @Test
    void acceptsExistingCnyAndUsdCurrencies() {
        var cnyProduct = product("9.90", "CNY", "ON_SHELF");
        var usdProduct = product("9.90", "USD", "OFF_SHELF");
        when(productRepo.save(any(AiServiceProduct.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        assertThat(productService.create(cnyProduct).getCurrency()).isEqualTo("CNY");
        assertThat(productService.save(usdProduct).getCurrency()).isEqualTo("USD");
        verify(productRepo, times(2)).save(any(AiServiceProduct.class));
        verify(sitemapVersion, times(2)).invalidate();
    }

    private AiServiceProduct product(String salePrice, String currency, String status) {
        var product = new AiServiceProduct();
        product.setProductCode("TEST_PRODUCT");
        product.setName("测试产品");
        product.setServiceType("GENERAL");
        product.setSalePrice(new BigDecimal(salePrice));
        product.setCurrency(currency);
        product.setStatus(status);
        product.setPeriodDays(30);
        product.setRequiredFieldsJson("[]");
        return product;
    }
}
