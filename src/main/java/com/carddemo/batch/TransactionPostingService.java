package com.carddemo.batch;

import com.carddemo.model.Account;
import com.carddemo.model.CardXref;
import com.carddemo.model.Transaction;
import com.carddemo.model.TransactionCategoryBalance;
import com.carddemo.model.TransactionCategoryBalanceId;
import com.carddemo.repository.AccountRepository;
import com.carddemo.repository.CardXrefRepository;
import com.carddemo.repository.TransactionCategoryBalanceRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

/** CBTRN02C, one DALYTRAN record at a time: 1500-VALIDATE-TRAN, then 2000-POST-TRANSACTION. */
@Service
public class TransactionPostingService {

    /** Z-GET-DB2-FORMAT-TIMESTAMP: YYYY-MM-DD-HH.MM.SS.hh0000 (hundredths, then '0000'). */
    static final DateTimeFormatter DB2_TIMESTAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH.mm.ss.SS'0000'");

    private final CardXrefRepository cardXrefs;
    private final AccountRepository accounts;
    private final TransactionCategoryBalanceRepository categoryBalances;
    private final EntityManager entityManager;

    public TransactionPostingService(CardXrefRepository cardXrefs,
                                     AccountRepository accounts,
                                     TransactionCategoryBalanceRepository categoryBalances,
                                     EntityManager entityManager) {
        this.cardXrefs = cardXrefs;
        this.accounts = accounts;
        this.categoryBalances = categoryBalances;
        this.entityManager = entityManager;
    }

    /** Posts the record, or returns why it was rejected (nothing is changed then). */
    @Transactional
    public Optional<PostingReject> post(Transaction daily) {
        // 1500-A-LOOKUP-XREF
        CardXref xref = cardXrefs.findById(daily.getCardNumber()).orElse(null);
        if (xref == null) {
            return Optional.of(PostingReject.INVALID_CARD);
        }
        // 1500-B-LOOKUP-ACCT
        Account account = accounts.findForUpdateById(xref.getAccountId()).orElse(null);
        if (account == null) {
            return Optional.of(PostingReject.ACCOUNT_NOT_FOUND);
        }
        // Both checks run; when both fail the later one (103) is the reason kept, as in the COBOL.
        PostingReject reject = null;
        BigDecimal tempBalance = account.getCurrentCycleCredit()
                .subtract(account.getCurrentCycleDebit())
                .add(daily.getAmount());
        if (account.getCreditLimit().compareTo(tempBalance) < 0) {
            reject = PostingReject.OVERLIMIT;
        }
        if (!receivedBeforeExpiry(account, daily)) {
            reject = PostingReject.EXPIRED;
        }
        if (reject != null) {
            return Optional.of(reject);
        }

        postTransaction(daily, account);
        return Optional.empty();
    }

    /** ACCT-EXPIRAION-DATE >= DALYTRAN-ORIG-TS (1:10), compared as text like the COBOL. */
    private static boolean receivedBeforeExpiry(Account account, Transaction daily) {
        String expiry = account.getExpirationDate() == null ? "" : account.getExpirationDate().toString();
        String ts = daily.getOriginTimestamp() == null ? "" : daily.getOriginTimestamp();
        return expiry.compareTo(ts.substring(0, Math.min(10, ts.length()))) >= 0;
    }

    /** 2000-POST-TRANSACTION: 2700 category balance, 2800 account, 2900 write TRANSACT. */
    private void postTransaction(Transaction daily, Account account) {
        BigDecimal amount = daily.getAmount();
        daily.setProcessedTimestamp(LocalDateTime.now().format(DB2_TIMESTAMP));

        var key = new TransactionCategoryBalanceId(account.getId(), daily.getTypeCode(), daily.getCategoryCode());
        categoryBalances.findById(key).ifPresentOrElse(
                balance -> balance.setBalance(balance.getBalance().add(amount)),
                () -> {
                    var balance = new TransactionCategoryBalance();
                    balance.setId(key);
                    balance.setBalance(amount);
                    entityManager.persist(balance);
                });

        account.setCurrentBalance(account.getCurrentBalance().add(amount));
        if (amount.signum() >= 0) {
            account.setCurrentCycleCredit(account.getCurrentCycleCredit().add(amount));
        } else {
            account.setCurrentCycleDebit(account.getCurrentCycleDebit().add(amount));
        }

        try {
            entityManager.persist(daily);
            entityManager.flush();
        } catch (PersistenceException e) {
            throw new PostingAbendException("ERROR WRITING TO TRANSACTION FILE: TRAN-ID " + daily.getId(), e);
        }
    }
}
