package com.wildai.fulfillment.repository;

import com.wildai.fulfillment.domain.FulfillmentLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FulfillmentLogRepository extends JpaRepository<FulfillmentLog, Long> {

    List<FulfillmentLog> findByTaskIdOrderByCreatedAtAsc(Long taskId);

    List<FulfillmentLog> findByTaskIdAndUserVisibleTrueOrderByCreatedAtAsc(Long taskId);
}
