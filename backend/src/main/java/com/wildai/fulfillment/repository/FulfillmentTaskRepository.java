package com.wildai.fulfillment.repository;

import com.wildai.fulfillment.domain.FulfillmentTask;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FulfillmentTaskRepository extends JpaRepository<FulfillmentTask, Long> {

    Optional<FulfillmentTask> findByOrderId(Long orderId);

    Optional<FulfillmentTask> findByTaskNo(String taskNo);
}
