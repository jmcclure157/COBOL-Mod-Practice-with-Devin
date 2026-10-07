package com.carddemo.repository;

import com.carddemo.model.CardXref;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CardXrefRepository extends JpaRepository<CardXref, String> {

    /** Mirrors the CICS READ of CXACAIX (alternate index by account id) in COACTVWC. */
    Optional<CardXref> findFirstByAccountIdOrderByCardNumberAsc(Long accountId);
}
