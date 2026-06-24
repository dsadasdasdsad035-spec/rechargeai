package com.wildai.finance.repository;

import com.wildai.finance.domain.PayoutAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PayoutAccountRepository extends JpaRepository<PayoutAccount, Long> {

    List<PayoutAccount> findByEnabledTrueOrderByIdAsc();
}
