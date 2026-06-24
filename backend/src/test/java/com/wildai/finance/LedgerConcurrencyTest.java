package com.wildai.finance;

import com.wildai.admin.repository.AdminUserRepository;
import com.wildai.common.exception.BusinessException;
import com.wildai.finance.domain.LedgerEntryType;
import com.wildai.finance.domain.WithdrawalStatus;
import com.wildai.finance.dto.CreateWithdrawalRequest;
import com.wildai.finance.dto.PayoutAccountCreateRequest;
import com.wildai.finance.repository.PlatformLedgerEntryRepository;
import com.wildai.finance.repository.WithdrawalRequestRepository;
import com.wildai.finance.service.LedgerService;
import com.wildai.finance.service.PayoutAccountService;
import com.wildai.finance.service.WithdrawalService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * SC-013：并发提现申请不得超额冻结或使余额为负。
 */
@SpringBootTest
@ActiveProfiles("smoke")
@Testcontainers
class LedgerConcurrencyTest {

    @Container
    static GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379);

    @Container
    @ServiceConnection
    static org.testcontainers.containers.MySQLContainer<?> mysql =
            new org.testcontainers.containers.MySQLContainer<>(DockerImageName.parse("mysql:8.0"))
                    .withDatabaseName("wildai");

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
        registry.add("wildai.finance.max-pending-withdrawals", () -> 20);
    }

    @Autowired LedgerService ledgerService;
    @Autowired WithdrawalService withdrawalService;
    @Autowired PayoutAccountService payoutAccountService;
    @Autowired PlatformLedgerEntryRepository entryRepo;
    @Autowired WithdrawalRequestRepository withdrawalRepo;
    @Autowired AdminUserRepository adminUserRepo;

    @Test
    @DisplayName("并发 10 笔提现申请：余额不为负且冻结总额不超过初始可用")
    void concurrentWithdrawalsDoNotOverFreeze() throws Exception {
        Long adminId = adminUserRepo.findByUsername("admin").orElseThrow().getId();
        BigDecimal initial = new BigDecimal("1000.00");
        ledgerService.recordSettle("O-CONC-" + System.currentTimeMillis(), null, initial);

        var account = payoutAccountService.create(adminId, new PayoutAccountCreateRequest(
                "测试户", "测试银行", "6222021234567890123"));

        int threads = 10;
        BigDecimal each = new BigDecimal("200.00");
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger success = new AtomicInteger();
        AtomicInteger failed = new AtomicInteger();
        List<Future<?>> futures = new ArrayList<>();

        for (int i = 0; i < threads; i++) {
            futures.add(pool.submit(() -> {
                start.await();
                try {
                    withdrawalService.create(adminId, new CreateWithdrawalRequest(
                            each, account.id(), "并发测试"));
                    success.incrementAndGet();
                } catch (BusinessException ex) {
                    failed.incrementAndGet();
                }
                return null;
            }));
        }

        start.countDown();
        for (Future<?> f : futures) {
            f.get();
        }
        pool.shutdown();

        assertThat(success.get()).isGreaterThan(0);
        assertThat(success.get() + failed.get()).isEqualTo(threads);

        BigDecimal available = ledgerService.getAvailableBalance();
        assertThat(available).isGreaterThanOrEqualTo(BigDecimal.ZERO);

        BigDecimal frozen = withdrawalRepo.sumAmountByStatusIn(
                List.of(WithdrawalStatus.PENDING_APPROVAL, WithdrawalStatus.APPROVED));
        assertThat(frozen).isLessThanOrEqualTo(initial);
        assertThat(frozen.add(available)).isLessThanOrEqualTo(initial);

        BigDecimal freezeLedger = entryRepo.sumAmountByEntryType(LedgerEntryType.WITHDRAW_FREEZE).abs();
        BigDecimal releaseLedger = entryRepo.sumAmountByEntryType(LedgerEntryType.WITHDRAW_RELEASE);
        assertThat(freezeLedger.subtract(releaseLedger)).isLessThanOrEqualTo(initial);
    }
}
