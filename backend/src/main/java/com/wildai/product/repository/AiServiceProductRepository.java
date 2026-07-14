package com.wildai.product.repository;

import com.wildai.product.domain.AiServiceProduct;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AiServiceProductRepository extends JpaRepository<AiServiceProduct, Long> {
    List<AiServiceProduct> findByStatusOrderBySortOrderAsc(String status);
    List<AiServiceProduct> findByStatusOrderByUpdatedAtDesc(String status);
    List<AiServiceProduct> findAllByOrderBySortOrderAsc();
    boolean existsByProductCode(String productCode);
}
