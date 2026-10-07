package com.carddemo.service;

import com.carddemo.model.Transaction;
import com.carddemo.repository.AccountRepository;
import com.carddemo.repository.CardXrefRepository;
import com.carddemo.repository.TransactionRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Runs against the seeded data; each test rolls back so balances and transactions stay as loaded. */
@SpringBootTest
@Transactional
class BillPaymentServiceTest {

    @Autowired BillPaymentService service;
    @Autowired AccountRepository accounts;
    @Autowired CardXrefRepository cardXrefs;
    @Autowired TransactionRepository transactions;
    @Autowired EntityManager entityManager;

    void rejected(String accountId, String confirm, Class<? extends RuntimeException> type, String message) {
        long before = transactions.count();
        assertThatThrownBy(() -> service.payBill(accountId, confirm)).isInstanceOf(type).hasMessage(message);
        assertThat(transactions.count()).isEqualTo(before);
    }

    void setBalance(long accountId, String balance) {
        accounts.findById(accountId).orElseThrow().setCurrentBalance(new BigDecimal(balance));
    }

    @Test
    void paysTheWholeBalanceAndWritesAPaymentTransaction() {
        var result = service.payBill("2", "Y");

        assertThat(result.transactionId()).isEqualTo("0000000996722788");
        assertThat(result.amountPaid()).isEqualByComparingTo("158.00");
        assertThat(result.newBalance()).isEqualByComparingTo("0.00");
        assertThat(result.message()).isEqualTo("Payment successful.  Your Transaction ID is 0000000996722788.");
        assertThat(accounts.findById(2L).orElseThrow().getCurrentBalance()).isEqualByComparingTo("0.00");

        Transaction t = transactions.findById(result.transactionId()).orElseThrow();
        assertThat(t.getTypeCode()).isEqualTo("02");
        assertThat(t.getCategoryCode()).isEqualTo(2);
        assertThat(t.getSource()).isEqualTo("POS TERM");
        assertThat(t.getDescription()).isEqualTo("BILL PAYMENT - ONLINE");
        assertThat(t.getAmount()).isEqualByComparingTo("158.00");
        assertThat(t.getCardNumber()).isEqualTo("0923877193247330");
        assertThat(t.getMerchantId()).isEqualTo(999999999L);
        assertThat(t.getMerchantName()).isEqualTo("BILL PAYMENT");
        assertThat(t.getMerchantCity()).isEqualTo("N/A");
        assertThat(t.getMerchantZip()).isEqualTo("N/A");
        assertThat(t.getOriginTimestamp()).matches("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}\\.000000");
        assertThat(t.getProcessedTimestamp()).isEqualTo(t.getOriginTimestamp());
    }

    @Test
    void lowercaseYAndPaddedAccountIdAlsoPay() {
        assertThat(service.payBill(" 00000000002 ", "y").amountPaid()).isEqualByComparingTo("158.00");
    }

    @Test
    void secondPaymentHasNothingToPay() {
        service.payBill("2", "Y");
        rejected("2", "Y", BillPaymentRejectedException.class, BillPaymentService.MSG_NOTHING_TO_PAY);
    }

    @Test
    void blankAccountIdIsRejected() {
        rejected(null, "Y", InvalidAccountIdException.class, BillPaymentService.MSG_ACCT_ID_EMPTY);
        rejected("   ", "Y", InvalidAccountIdException.class, BillPaymentService.MSG_ACCT_ID_EMPTY);
    }

    @Test
    void unknownOrNonNumericAccountIsNotFound() {
        rejected("99999999999", "Y", AccountNotFoundException.class, BillPaymentService.MSG_ACCOUNT_NOT_FOUND);
        rejected("abc", "Y", AccountNotFoundException.class, BillPaymentService.MSG_ACCOUNT_NOT_FOUND);
        rejected("123456789012", "Y", AccountNotFoundException.class, BillPaymentService.MSG_ACCOUNT_NOT_FOUND);
    }

    @Test
    void invalidConfirmIsCheckedBeforeTheAccountIsRead() {
        rejected("99999999999", "X", BillPaymentRejectedException.class, BillPaymentService.MSG_CONFIRM_INVALID);
    }

    @Test
    void blankOrNoConfirmAsksToConfirmAndChangesNothing() {
        for (String confirm : new String[] {null, "", " ", "N", "n"}) {
            rejected("2", confirm, BillPaymentRejectedException.class, BillPaymentService.MSG_CONFIRM);
        }
        assertThat(accounts.findById(2L).orElseThrow().getCurrentBalance()).isEqualByComparingTo("158.00");
    }

    @Test
    void zeroOrNegativeBalanceHasNothingToPayEvenBeforeConfirming() {
        setBalance(2, "0.00");
        rejected("2", "", BillPaymentRejectedException.class, BillPaymentService.MSG_NOTHING_TO_PAY);
        setBalance(2, "-25.00");
        rejected("2", "Y", BillPaymentRejectedException.class, BillPaymentService.MSG_NOTHING_TO_PAY);
    }

    @Test
    void accountWithoutACardIsNotFound() {
        cardXrefs.deleteAll(cardXrefs.findAll().stream().filter(x -> x.getAccountId() == 2L).toList());
        rejected("2", "Y", AccountNotFoundException.class, BillPaymentService.MSG_ACCOUNT_NOT_FOUND);
        assertThat(accounts.findById(2L).orElseThrow().getCurrentBalance()).isEqualByComparingTo("158.00");
    }

    @Test
    void refusesWithTheBillPayMessageOnceTheHighest16DigitIdIsTaken() {
        var full = new Transaction();
        full.setId("9999999999999999");
        entityManager.persist(full);
        assertThatThrownBy(() -> service.payBill("2", "Y"))
                .isInstanceOf(TransactionIdsExhaustedException.class)
                .hasMessage(BillPaymentService.MSG_UNABLE_TO_ADD);
        assertThat(accounts.findById(2L).orElseThrow().getCurrentBalance()).isEqualByComparingTo("158.00");
    }

    @Test
    void largestBalanceThatFitsTheTransactionAmountIsPaid() {
        setBalance(2, "999999999.99");
        assertThat(service.payBill("2", "Y").amountPaid()).isEqualByComparingTo("999999999.99");
    }

    @Test
    void balanceTooLargeForTheTransactionAmountIsRefusedAndKept() {
        setBalance(2, "1000000000.00");
        rejected("2", "Y", BillPaymentRejectedException.class, BillPaymentService.MSG_UNABLE_TO_ADD);
        assertThat(accounts.findById(2L).orElseThrow().getCurrentBalance()).isEqualByComparingTo("1000000000.00");
    }

    @Test
    void newIdUsesAsciiDigitsWhateverTheServerLocale() {
        Locale original = Locale.getDefault();
        try {
            Locale.setDefault(Locale.forLanguageTag("ar-EG"));
            assertThat(service.payBill("2", "Y").transactionId()).isEqualTo("0000000996722788");
        } finally {
            Locale.setDefault(original);
        }
    }
}
