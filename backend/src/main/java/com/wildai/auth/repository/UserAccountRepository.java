package com.wildai.auth.repository;

import com.wildai.auth.domain.UserAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserAccountRepository extends JpaRepository<UserAccount, Long> {
    Optional<UserAccount> findByPhone(String phone);
    Optional<UserAccount> findByEmail(String email);
    Optional<UserAccount> findByUserNo(String userNo);
}
