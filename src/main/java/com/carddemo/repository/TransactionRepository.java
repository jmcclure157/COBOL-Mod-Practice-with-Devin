package com.carddemo.repository;

import com.carddemo.model.Transaction;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TransactionRepository extends JpaRepository<Transaction, String> {

    /** COTRN00C browse: STARTBR TRANSACT at a key (GTEQ), then READNEXT in key order. */
    Slice<Transaction> findByIdGreaterThanEqualOrderByIdAsc(String startId, Pageable pageable);

    /** COTRN02C: STARTBR TRANSACT at HIGH-VALUES, then READPREV = the record with the highest id. */
    Optional<Transaction> findTopByOrderByIdDesc();
}
