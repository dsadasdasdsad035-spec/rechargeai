package com.wildai.finance.repository;

import com.wildai.finance.domain.WithdrawalRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface WithdrawalRequestRepository extends JpaRepository<WithdrawalRequest, Long> {

    Optional<WithdrawalRequest> findByWithdrawalNo(String withdrawalNo);

    long countByStatusIn(Collection<String> statuses);

    @Query("SELECT COALESCE(SUM(w.amount), 0) FROM WithdrawalRequest w WHERE w.status IN :statuses")
    BigDecimal sumAmountByStatusIn(@Param("statuses") Collection<String> statuses);

    @Query("SELECT COALESCE(SUM(COALESCE(w.actualAmount, w.amount)), 0) FROM WithdrawalRequest w WHERE w.status = 'COMPLETED'")
    BigDecimal sumCompletedWithdrawn();

    @Query("""
            SELECT COALESCE(SUM(w.amount), 0) FROM WithdrawalRequest w
            WHERE w.applicantAdminId = :adminId
            AND w.appliedAt >= :dayStart
            AND w.status NOT IN ('REJECTED', 'CANCELLED')
            """)
    BigDecimal sumTodayAppliedByApplicant(@Param("adminId") Long adminId, @Param("dayStart") Instant dayStart);

    @Query("""
            SELECT w FROM WithdrawalRequest w WHERE
            (:status IS NULL OR w.status = :status)
            AND (:withdrawalNo IS NULL OR w.withdrawalNo = :withdrawalNo)
            ORDER BY w.appliedAt DESC
            """)
    Page<WithdrawalRequest> search(@Param("status") String status,
                                   @Param("withdrawalNo") String withdrawalNo,
                                   Pageable pageable);

    List<WithdrawalRequest> findByStatusAndApprovedAtBefore(String status, Instant before);
}
