package com.carddemo.service;

import com.carddemo.model.Transaction;
import com.carddemo.repository.TransactionRepository;
import jakarta.persistence.EntityExistsException;
import jakarta.persistence.EntityManager;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.stereotype.Component;

import java.util.Locale;

/**
 * The "next id + WRITE TRANSACT" steps shared by COTRN02C (add transaction) and COBIL00C (bill payment):
 * STARTBR at HIGH-VALUES, READPREV for the highest TRAN-ID, ADD 1, then WRITE (which fails on DUPREC).
 */
@Component
public class TransactionWriter {

    static final String MSG_DUPLICATE = "Tran ID already exist...";

    private static final long MAX_TRAN_ID = 9_999_999_999_999_999L;

    private final TransactionRepository transactions;
    private final EntityManager entityManager;

    public TransactionWriter(TransactionRepository transactions, EntityManager entityManager) {
        this.transactions = transactions;
        this.entityManager = entityManager;
    }

    /**
     * Gives {@code t} the next id and inserts it. Uses persist (insert-only), not save, which would merge
     * over an existing row with the same id. Must run inside the caller's transaction.
     *
     * @param unableToAddMessage the caller's COBOL message for a WRITE that cannot be done
     * @return the new 16-digit id
     */
    public String insertWithNextId(Transaction t, String unableToAddMessage) {
        t.setId(nextTransactionId(unableToAddMessage));
        try {
            entityManager.persist(t);
            entityManager.flush();
        } catch (EntityExistsException | ConstraintViolationException e) {
            throw new DuplicateTransactionIdException(MSG_DUPLICATE);
        }
        return t.getId();
    }

    /** READPREV from HIGH-VALUES finds the highest id (ZEROS if the file is empty), then add 1. */
    private String nextTransactionId(String unableToAddMessage) {
        long highest = transactions.findTopByOrderByIdDesc()
                .map(t -> Long.parseLong(t.getId()))
                .orElse(0L);
        // COBOL's ADD 1 would silently wrap a full PIC 9(16) to zeros; refuse instead.
        if (highest >= MAX_TRAN_ID) {
            throw new TransactionIdsExhaustedException(unableToAddMessage);
        }
        return String.format(Locale.ROOT, "%016d", highest + 1);
    }
}
