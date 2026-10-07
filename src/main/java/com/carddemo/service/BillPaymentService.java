package com.carddemo.service;

import com.carddemo.model.Account;
import com.carddemo.model.CardXref;
import com.carddemo.model.Transaction;
import com.carddemo.repository.AccountRepository;
import com.carddemo.repository.CardXrefRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Business logic of CICS program COBIL00C (transaction CB00, "Bill Payment"), without the screen handling.
 * Paying writes a payment transaction for the whole current balance and sets the balance to zero.
 * Both changes happen in one database transaction, so either both are saved or neither is.
 */
@Service
public class BillPaymentService {

    static final String MSG_ACCT_ID_EMPTY = "Acct ID can NOT be empty...";
    static final String MSG_ACCOUNT_NOT_FOUND = "Account ID NOT found...";
    static final String MSG_CONFIRM_INVALID = "Invalid value. Valid values are (Y/N)...";
    static final String MSG_NOTHING_TO_PAY = "You have nothing to pay...";
    static final String MSG_CONFIRM = "Confirm to make a bill payment...";
    static final String MSG_UNABLE_TO_ADD = "Unable to Add Bill pay Transaction...";

    /** GET-CURRENT-TIMESTAMP: date, time, and the microseconds always ZEROS. */
    static final DateTimeFormatter TIMESTAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.000000");

    private final AccountRepository accounts;
    private final CardXrefRepository cardXrefs;
    private final TransactionWriter writer;

    public BillPaymentService(AccountRepository accounts, CardXrefRepository cardXrefs, TransactionWriter writer) {
        this.accounts = accounts;
        this.cardXrefs = cardXrefs;
        this.writer = writer;
    }

    /** PROCESS-ENTER-KEY, in the COBOL order. */
    @Transactional
    public BillPaymentResult payBill(String rawAccountId, String rawConfirm) {
        String accountIdText = rawAccountId == null ? "" : rawAccountId.strip();
        if (accountIdText.isEmpty()) {
            throw new InvalidAccountIdException(MSG_ACCT_ID_EMPTY);
        }
        boolean confirmed = editConfirm(rawConfirm);

        // READ-ACCTDAT-FILE ... UPDATE: read and lock the account until the payment is saved.
        Account account = accounts.findForUpdateById(parseAccountId(accountIdText))
                .orElseThrow(() -> new AccountNotFoundException(MSG_ACCOUNT_NOT_FOUND));
        if (account.getCurrentBalance().signum() <= 0) {
            throw new BillPaymentRejectedException(MSG_NOTHING_TO_PAY);
        }
        if (!confirmed) {
            throw new BillPaymentRejectedException(MSG_CONFIRM);
        }

        // READ-CXACAIX-FILE: the account's card goes on the payment transaction.
        CardXref xref = cardXrefs.findFirstByAccountIdOrderByCardNumberAsc(account.getId())
                .orElseThrow(() -> new AccountNotFoundException(MSG_ACCOUNT_NOT_FOUND));

        BigDecimal amount = account.getCurrentBalance();
        String now = LocalDateTime.now().format(TIMESTAMP);
        var payment = new Transaction();
        payment.setTypeCode("02");
        payment.setCategoryCode(2);
        payment.setSource("POS TERM");
        payment.setDescription("BILL PAYMENT - ONLINE");
        payment.setAmount(amount);
        payment.setCardNumber(xref.getCardNumber());
        payment.setMerchantId(999999999L);
        payment.setMerchantName("BILL PAYMENT");
        payment.setMerchantCity("N/A");
        payment.setMerchantZip("N/A");
        payment.setOriginTimestamp(now);
        payment.setProcessedTimestamp(now);
        String id = writer.insertWithNextId(payment, MSG_UNABLE_TO_ADD);

        // UPDATE-ACCTDAT-FILE (REWRITE): the managed entity is saved when the transaction commits.
        account.setCurrentBalance(account.getCurrentBalance().subtract(amount));

        return new BillPaymentResult(id, amount, account.getCurrentBalance(),
                "Payment successful.  Your Transaction ID is " + id + ".");
    }

    /** CONFIRM: Y pays; N or blank only shows the balance and asks to confirm; anything else is invalid. */
    private static boolean editConfirm(String rawConfirm) {
        String confirm = rawConfirm == null ? "" : rawConfirm.strip();
        return switch (confirm) {
            case "Y", "y" -> true;
            case "N", "n", "" -> false;
            default -> throw new BillPaymentRejectedException(MSG_CONFIRM_INVALID);
        };
    }

    /** ACCT-ID is PIC 9(11). The COBOL has no numeric edit here, so a non-number simply is not found. */
    private static long parseAccountId(String value) {
        if (value.length() > 11 || !value.chars().allMatch(ch -> ch >= '0' && ch <= '9')) {
            throw new AccountNotFoundException(MSG_ACCOUNT_NOT_FOUND);
        }
        return Long.parseLong(value);
    }
}
