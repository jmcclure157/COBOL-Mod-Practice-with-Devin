package com.carddemo.repository;

import com.carddemo.model.Account;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.Optional;

public interface AccountRepository extends JpaRepository<Account, Long> {

    /** CICS READ ... UPDATE: read the account and lock it until the transaction ends. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Account> findForUpdateById(Long id);
}
