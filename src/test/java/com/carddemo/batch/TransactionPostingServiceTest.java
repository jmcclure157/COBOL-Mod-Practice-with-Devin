package com.carddemo.batch;

import com.carddemo.model.Account;
import com.carddemo.model.CardXref;
import com.carddemo.model.Transaction;
import com.carddemo.model.TransactionCategoryBalanceId;
import com.carddemo.repository.AccountRepository;
import com.carddemo.repository.CardXrefRepository;
import com.carddemo.repository.TransactionCategoryBalanceRepository;
import com.carddemo.repository.TransactionRepository;
import com.carddemo.seed.CardDemoDataLoader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * One record at a time against the data as posted at startup. Account 1 then has credit limit 2020.00,
 * cycle credit 1164.87, cycle debit -70.77 (so 784.36 more is exactly at the limit) and expires 2025-05-20.
 * Each test rolls back.
 */
@SpringBootTest
@Transactional
class TransactionPostingServiceTest {

    static final String RECEIVED = "2022-06-10 19:27:53.000000";

    @Autowired TransactionPostingService posting;
    @Autowired AccountRepository accounts;
    @Autowired CardXrefRepository cardXrefs;
    @Autowired TransactionCategoryBalanceRepository categoryBalances;
    @Autowired TransactionRepository transactions;

    String accountOneCard;

    @BeforeEach
    void findAccountOneCard() {
        accountOneCard = cardXrefs.findFirstByAccountIdOrderByCardNumberAsc(1L).orElseThrow().getCardNumber();
    }

    static Transaction daily(String id, String card, long cents, String received) {
        return CardDemoDataLoader.parseTransaction(DailyRecords.line(id, card, cents, received));
    }

    Account accountOne() {
        return accounts.findById(1L).orElseThrow();
    }

    void assertRejected(Transaction daily, PostingReject expected) {
        BigDecimal balanceBefore = accountOne().getCurrentBalance();
        long countBefore = transactions.count();
        assertThat(posting.post(daily)).contains(expected);
        assertThat(transactions.count()).isEqualTo(countBefore);
        assertThat(accountOne().getCurrentBalance()).isEqualByComparingTo(balanceBefore);
    }

    @Test
    void validRecordIsWrittenAndBothBalancesGoUp() {
        var key = new TransactionCategoryBalanceId(1L, "01", 1);
        BigDecimal categoryBefore = categoryBalances.findById(key).orElseThrow().getBalance();

        assertThat(posting.post(daily("0000000999999001", accountOneCard, 1000, RECEIVED))).isEmpty();

        Transaction saved = transactions.findById("0000000999999001").orElseThrow();
        assertThat(saved.getAmount()).isEqualByComparingTo("10.00");
        assertThat(saved.getCardNumber()).isEqualTo(accountOneCard);
        assertThat(saved.getOriginTimestamp()).isEqualTo(RECEIVED);
        assertThat(saved.getProcessedTimestamp()).matches("\\d{4}-\\d\\d-\\d\\d-\\d\\d\\.\\d\\d\\.\\d\\d\\.\\d\\d0000");

        Account account = accountOne();
        assertThat(account.getCurrentBalance()).isEqualByComparingTo("1298.10");
        assertThat(account.getCurrentCycleCredit()).isEqualByComparingTo("1174.87");
        assertThat(account.getCurrentCycleDebit()).isEqualByComparingTo("-70.77");
        assertThat(categoryBalances.findById(key).orElseThrow().getBalance())
                .isEqualByComparingTo(categoryBefore.add(new BigDecimal("10.00")));
    }

    @Test
    void negativeAmountIsAddedToTheCycleDebit() {
        assertThat(posting.post(daily("0000000999999002", accountOneCard, -2500, RECEIVED))).isEmpty();

        Account account = accountOne();
        assertThat(account.getCurrentBalance()).isEqualByComparingTo("1263.10");
        assertThat(account.getCurrentCycleCredit()).isEqualByComparingTo("1164.87");
        assertThat(account.getCurrentCycleDebit()).isEqualByComparingTo("-95.77");
    }

    @Test
    void missingCategoryBalanceIsCreated() {
        Transaction t = daily("0000000999999003", accountOneCard, 1234, RECEIVED);
        t.setCategoryCode(4321);
        var key = new TransactionCategoryBalanceId(1L, "01", 4321);
        assertThat(categoryBalances.findById(key)).isEmpty();

        assertThat(posting.post(t)).isEmpty();

        assertThat(categoryBalances.findById(key).orElseThrow().getBalance()).isEqualByComparingTo("12.34");
    }

    @Test
    void unknownCardIsReason100() {
        assertRejected(daily("0000000999999004", "0000000000000000", 1000, RECEIVED), PostingReject.INVALID_CARD);
    }

    @Test
    void cardWhoseAccountIsMissingIsReason101() {
        var xref = new CardXref();
        xref.setCardNumber("9999999999999999");
        xref.setCustomerId(1L);
        xref.setAccountId(99999999999L);
        cardXrefs.save(xref);

        assertThat(posting.post(daily("0000000999999005", "9999999999999999", 1000, RECEIVED)))
                .contains(PostingReject.ACCOUNT_NOT_FOUND);
    }

    @Test
    void reachingTheCreditLimitIsAllowedButGoingOverIsReason102() {
        assertRejected(daily("0000000999999006", accountOneCard, 78437, RECEIVED), PostingReject.OVERLIMIT);
        assertThat(posting.post(daily("0000000999999007", accountOneCard, 78436, RECEIVED))).isEmpty();
    }

    @Test
    void receivedAfterTheExpiryDateIsReason103() {
        assertRejected(daily("0000000999999008", accountOneCard, 1000, "2025-05-21 00:00:00.000000"),
                PostingReject.EXPIRED);
        assertThat(posting.post(daily("0000000999999009", accountOneCard, 1000, "2025-05-20 23:59:59.000000")))
                .isEmpty();
    }

    @Test
    void whenOverLimitAndExpiredTheExpiryReasonWins() {
        assertRejected(daily("0000000999999010", accountOneCard, 99999, "2025-05-21 00:00:00.000000"),
                PostingReject.EXPIRED);
    }

    @Test
    void existingTransactionIdAbendsTheJob() {
        assertThatThrownBy(() -> posting.post(daily("0000000000683580", accountOneCard, 1000, RECEIVED)))
                .isInstanceOf(PostingAbendException.class)
                .hasMessage("ERROR WRITING TO TRANSACTION FILE: TRAN-ID 0000000000683580");
    }

    @Test
    void rejectTrailerIsFourDigitCodeAndSeventySixCharacterReason() {
        String trailer = PostingReject.OVERLIMIT.trailer();
        assertThat(trailer).hasSize(80).startsWith("0102OVERLIMIT TRANSACTION ");
        assertThat(Optional.of(trailer.strip())).contains("0102OVERLIMIT TRANSACTION");
    }
}
