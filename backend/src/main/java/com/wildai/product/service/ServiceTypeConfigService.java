package com.wildai.product.service;

import com.wildai.common.exception.BusinessException;
import com.wildai.common.exception.ErrorCode;
import com.wildai.product.domain.ServiceTypeConfig;
import com.wildai.product.dto.ServiceTypeConfigUpdateRequest;
import com.wildai.product.dto.ServiceTypeGuideDto;
import com.wildai.product.repository.ServiceTypeConfigRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
public class ServiceTypeConfigService {

    private final ServiceTypeConfigRepository configRepo;

    public ServiceTypeConfigService(ServiceTypeConfigRepository configRepo) {
        this.configRepo = configRepo;
    }

    public List<ServiceTypeGuideDto> listAll() {
        return configRepo.findAll().stream().map(this::toDto).toList();
    }

    public ServiceTypeGuideDto getGuide(String serviceType) {
        return configRepo.findById(serviceType)
                .map(this::toDto)
                .orElse(fallbackGuide(serviceType));
    }

    @Transactional
    public ServiceTypeGuideDto update(String serviceType, ServiceTypeConfigUpdateRequest req) {
        if (req.displayName() == null || req.displayName().isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "请填写显示名称");
        }
        ServiceTypeConfig config = configRepo.findById(serviceType).orElseGet(() -> {
            ServiceTypeConfig created = new ServiceTypeConfig();
            created.setServiceType(serviceType);
            created.setCreatedAt(Instant.now());
            return created;
        });
        config.setDisplayName(req.displayName().trim());
        config.setAccountTutorial(trimToNull(req.accountTutorial()));
        config.setTokenTutorial(trimToNull(req.tokenTutorial()));
        config.setUpdatedAt(Instant.now());
        return toDto(configRepo.save(config));
    }

    private ServiceTypeGuideDto fallbackGuide(String serviceType) {
        return new ServiceTypeGuideDto(serviceType, serviceType, null, null);
    }

    private ServiceTypeGuideDto toDto(ServiceTypeConfig config) {
        return new ServiceTypeGuideDto(
                config.getServiceType(),
                config.getDisplayName(),
                config.getAccountTutorial(),
                config.getTokenTutorial());
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
