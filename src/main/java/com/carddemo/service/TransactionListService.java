package com.carddemo.service;

import com.carddemo.model.Transaction;
import com.carddemo.repository.TransactionRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

/**
 * Business logic of CICS program COTRN00C (transaction CT00, "List Transactions"), without the screen handling.
 * The COBOL browse (STARTBR + READNEXT x 10, one extra READNEXT to decide if PF8 has a next page) becomes
 * a Spring Data {@code Slice} of 10 rows; PF7/PF8 become the {@code page} parameter.
 */
@Service
public class TransactionListService {

    static final int PAGE_SIZE = 10;
    static final String MSG_TRAN_ID_NOT_NUMERIC = "Tran ID must be Numeric ...";
    static final String MSG_PAGE_INVALID = "Page must be a positive number";

    private static final int TRAN_ID_LENGTH = 16;
    private static final int MAX_PAGE_DIGITS = 9;

    private final TransactionRepository transactions;

    public TransactionListService(TransactionRepository transactions) {
        this.transactions = transactions;
    }

    @Transactional(readOnly = true)
    public TransactionPage listTransactions(String rawStartId, String rawPage) {
        String startId = editStartId(rawStartId);
        int page = editPage(rawPage);
        var slice = transactions.findByIdGreaterThanEqualOrderByIdAsc(startId, PageRequest.of(page - 1, PAGE_SIZE));
        return new TransactionPage(page, slice.hasNext(), slice.map(TransactionListService::toItem).getContent());
    }

    /**
     * PROCESS-ENTER-KEY: a blank "Search Tran ID" starts at the top (LOW-VALUES); otherwise it must be numeric.
     * TRAN-ID is PIC X(16) holding zero-padded digits, so shorter input is left-padded with zeros.
     */
    private static String editStartId(String rawStartId) {
        String value = rawStartId == null ? "" : rawStartId.strip();
        if (value.isEmpty()) {
            return "";
        }
        if (value.length() > TRAN_ID_LENGTH || !isDigits(value)) {
            throw new InvalidTransactionListRequestException(MSG_TRAN_ID_NOT_NUMERIC);
        }
        return "0".repeat(TRAN_ID_LENGTH - value.length()) + value;
    }

    private static int editPage(String rawPage) {
        String value = rawPage == null ? "" : rawPage.strip();
        if (value.isEmpty()) {
            return 1;
        }
        if (value.length() > MAX_PAGE_DIGITS || !isDigits(value) || Integer.parseInt(value) < 1
                || (Integer.parseInt(value) - 1L) * PAGE_SIZE > Integer.MAX_VALUE) {
            throw new InvalidTransactionListRequestException(MSG_PAGE_INVALID);
        }
        return Integer.parseInt(value);
    }

    private static boolean isDigits(String value) {
        return value.chars().allMatch(ch -> ch >= '0' && ch <= '9');
    }

    /** POPULATE-TRAN-DATA: id, date part of TRAN-ORIG-TS, description, amount. */
    private static TransactionListItem toItem(Transaction t) {
        return new TransactionListItem(t.getId(), datePart(t.getOriginTimestamp()), t.getDescription(), t.getAmount());
    }

    /** The yyyy-MM-dd part of a TRAN-ORIG-TS / TRAN-PROC-TS timestamp, as the screens show it. */
    static LocalDate datePart(String timestamp) {
        return timestamp == null || timestamp.length() < 10 ? null : LocalDate.parse(timestamp.substring(0, 10));
    }
}
