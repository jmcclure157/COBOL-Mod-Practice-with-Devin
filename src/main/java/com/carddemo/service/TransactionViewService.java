package com.carddemo.service;

import com.carddemo.model.Transaction;
import com.carddemo.repository.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Business logic of CICS program COTRN01C (transaction CT01, "View Transaction"), without the screen handling.
 * PROCESS-ENTER-KEY rejects a blank ID, then READ-TRANSACT-FILE looks the record up by its exact 16-character key.
 * Only trailing spaces are dropped (COBOL pads short input with them); a leading space is part of the key.
 */
@Service
public class TransactionViewService {

    static final String MSG_TRAN_ID_EMPTY = "Tran ID can NOT be empty...";
    static final String MSG_TRAN_ID_NOT_FOUND = "Transaction ID NOT found...";

    private final TransactionRepository transactions;

    public TransactionViewService(TransactionRepository transactions) {
        this.transactions = transactions;
    }

    @Transactional(readOnly = true)
    public TransactionView viewTransaction(String rawId) {
        String id = rawId == null ? "" : rawId.stripTrailing();
        if (id.isBlank()) {
            throw new InvalidTransactionIdException(MSG_TRAN_ID_EMPTY);
        }
        return transactions.findById(id)
                .map(TransactionViewService::toView)
                .orElseThrow(() -> new TransactionNotFoundException(MSG_TRAN_ID_NOT_FOUND));
    }

    private static TransactionView toView(Transaction t) {
        return new TransactionView(
                t.getId(),
                t.getCardNumber(),
                t.getTypeCode(),
                t.getCategoryCode(),
                t.getSource(),
                t.getAmount(),
                t.getDescription(),
                TransactionListService.datePart(t.getOriginTimestamp()),
                TransactionListService.datePart(t.getProcessedTimestamp()),
                t.getMerchantId(),
                t.getMerchantName(),
                t.getMerchantCity(),
                t.getMerchantZip());
    }
}
