package com.wildai.product.repository;

import com.wildai.product.domain.ServiceTypeConfig;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ServiceTypeConfigRepository extends JpaRepository<ServiceTypeConfig, String> {
}
