package com.wildai.finance.repository;

import com.wildai.finance.domain.PlatformLedgerEntry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

public interface PlatformLedgerEntryRepository extends JpaRepository<PlatformLedgerEntry, Long> {

    boolean existsByEntryTypeAndRefTypeAndRefId(String entryType, String refType, String refId);

    boolean existsByEntryTypeAndOrderId(String entryType, Long orderId);

    Optional<PlatformLedgerEntry> findTopByOrderByIdDesc();

    @Query("SELECT COALESCE(SUM(e.amount), 0) FROM PlatformLedgerEntry e")
    BigDecimal sumAllAmounts();

    @Query(value = "SELECT COALESCE(SUM(amount), 0) FROM platform_ledger_entry", nativeQuery = true)
    BigDecimal sumAllAmountsNative();

    @Query("SELECT COALESCE(SUM(e.amount), 0) FROM PlatformLedgerEntry e WHERE e.entryType = :entryType")
    BigDecimal sumAmountByEntryType(@Param("entryType") String entryType);

    @Query("""
            SELECT e FROM PlatformLedgerEntry e WHERE
            (:entryType IS NULL OR e.entryType = :entryType)
            AND (:start IS NULL OR e.createdAt >= :start)
            AND (:end IS NULL OR e.createdAt <= :end)
            ORDER BY e.createdAt DESC
            """)
    Page<PlatformLedgerEntry> search(@Param("entryType") String entryType,
                                     @Param("start") Instant start,
                                     @Param("end") Instant end,
                                     Pageable pageable);
}
