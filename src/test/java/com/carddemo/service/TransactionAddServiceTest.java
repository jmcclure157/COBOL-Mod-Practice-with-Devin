package com.carddemo.service;

import com.carddemo.model.Transaction;
import com.carddemo.repository.TransactionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import jakarta.persistence.EntityManager;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.function.UnaryOperator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Runs against the seeded data; each test rolls back so other tests still see the original 300 transactions. */
@SpringBootTest
@Transactional
class TransactionAddServiceTest {

    @Autowired TransactionAddService service;
    @Autowired TransactionRepository transactions;
    @Autowired EntityManager entityManager;

    static NewTransactionRequest valid() {
        return new NewTransactionRequest("50", null, "01", "0001", "POS TERM", "Purchase at Test Store",
                "+00000123.45", "2026-10-07", "2026-10-07", "800000000", "Test Store", "Dallas", "75201", "Y");
    }

    void rejected(NewTransactionRequest request, Class<? extends RuntimeException> type, String message) {
        assertThatThrownBy(() -> service.addTransaction(request)).isInstanceOf(type).hasMessage(message);
    }

    @Test
    void addsWithTheNextIdAndTheAccountsCard() {
        var result = service.addTransaction(valid());
        assertThat(result.transactionId()).isEqualTo("0000000996722788");
        assertThat(result.message())
                .isEqualTo("Transaction added successfully.  Your Tran ID is 0000000996722788.");

        var saved = transactions.findById("0000000996722788").orElseThrow();
        assertThat(saved.getCardNumber()).isEqualTo("0500024453765740");
        assertThat(saved.getTypeCode()).isEqualTo("01");
        assertThat(saved.getCategoryCode()).isEqualTo(1);
        assertThat(saved.getAmount()).isEqualByComparingTo("123.45");
        assertThat(saved.getOriginTimestamp()).isEqualTo("2026-10-07");
        assertThat(saved.getMerchantId()).isEqualTo(800000000L);
        assertThat(transactions.count()).isEqualTo(301);

        assertThat(service.addTransaction(valid()).transactionId()).isEqualTo("0000000996722789");
    }

    @Test
    void cardNumberIsUsedWhenNoAccountIsGivenAndAccountWinsOverCard() {
        var byCard = new NewTransactionRequest(null, "683586198171516", "01", "0001", "POS TERM", "x",
                "-00000010.00", "2026-10-07", "2026-10-07", "1", "m", "c", "z", "y");
        String id = service.addTransaction(byCard).transactionId();
        assertThat(transactions.findById(id).orElseThrow().getCardNumber()).isEqualTo("0683586198171516");
        assertThat(transactions.findById(id).orElseThrow().getAmount()).isEqualByComparingTo("-10.00");

        var both = new NewTransactionRequest("50", "0683586198171516", "01", "0001", "POS TERM", "x",
                "+00000001.00", "2026-10-07", "2026-10-07", "1", "m", "c", "z", "Y");
        String id2 = service.addTransaction(both).transactionId();
        assertThat(transactions.findById(id2).orElseThrow().getCardNumber()).isEqualTo("0500024453765740");
    }

    @Test
    void keyFieldEdits() {
        rejected(with(r -> copy(r, "accountId", null)), InvalidNewTransactionException.class,
                TransactionAddService.MSG_KEY_MISSING);
        rejected(with(r -> copy(r, "accountId", "abc")), InvalidNewTransactionException.class,
                TransactionAddService.MSG_ACCOUNT_NOT_NUMERIC);
        rejected(with(r -> copy(r, "accountId", "99999")), CardOrAccountNotFoundException.class,
                TransactionAddService.MSG_ACCOUNT_NOT_FOUND);
        var noAccount = with(r -> copy(r, "accountId", ""));
        rejected(copy(noAccount, "cardNumber", "12x"), InvalidNewTransactionException.class,
                TransactionAddService.MSG_CARD_NOT_NUMERIC);
        rejected(copy(noAccount, "cardNumber", "1234"), CardOrAccountNotFoundException.class,
                TransactionAddService.MSG_CARD_NOT_FOUND);
    }

    @Test
    void requiredFieldsAreCheckedInTheCobolOrder() {
        var labels = new LinkedHashMap<String, String>();
        labels.put("typeCode", "Type CD");
        labels.put("categoryCode", "Category CD");
        labels.put("source", "Source");
        labels.put("description", "Description");
        labels.put("amount", "Amount");
        labels.put("originDate", "Orig Date");
        labels.put("processedDate", "Proc Date");
        labels.put("merchantId", "Merchant ID");
        labels.put("merchantName", "Merchant Name");
        labels.put("merchantCity", "Merchant City");
        labels.put("merchantZip", "Merchant Zip");
        labels.forEach((field, label) ->
                rejected(copy(valid(), field, " "), InvalidNewTransactionException.class,
                        label + " can NOT be empty..."));

        var allBlank = valid();
        for (String field : labels.keySet()) {
            allBlank = copy(allBlank, field, "");
        }
        rejected(allBlank, InvalidNewTransactionException.class, "Type CD can NOT be empty...");
    }

    @Test
    void formatEdits() {
        rejected(copy(valid(), "typeCode", "A1"), InvalidNewTransactionException.class,
                TransactionAddService.MSG_TYPE_NOT_NUMERIC);
        rejected(copy(valid(), "categoryCode", "00x1"), InvalidNewTransactionException.class,
                TransactionAddService.MSG_CATEGORY_NOT_NUMERIC);
        for (String bad : new String[] {"123.45", "+1234567.89", "+12345678,90", "00000123.45", "+00000123.4"}) {
            rejected(copy(valid(), "amount", bad), InvalidNewTransactionException.class,
                    TransactionAddService.MSG_AMOUNT_FORMAT);
        }
        rejected(copy(valid(), "originDate", "2026/10/07"), InvalidNewTransactionException.class,
                TransactionAddService.MSG_ORIG_DATE_FORMAT);
        rejected(copy(valid(), "processedDate", "10-07-2026"), InvalidNewTransactionException.class,
                TransactionAddService.MSG_PROC_DATE_FORMAT);
        rejected(copy(valid(), "originDate", "2026-02-30"), InvalidNewTransactionException.class,
                TransactionAddService.MSG_ORIG_DATE_INVALID);
        rejected(copy(valid(), "processedDate", "2026-13-01"), InvalidNewTransactionException.class,
                TransactionAddService.MSG_PROC_DATE_INVALID);
        rejected(copy(valid(), "merchantId", "abc"), InvalidNewTransactionException.class,
                TransactionAddService.MSG_MERCHANT_ID_NOT_NUMERIC);
        rejected(copy(valid(), "description", "x".repeat(101)), InvalidNewTransactionException.class,
                "Description must be at most 100 characters");
    }

    @Test
    void mustBeConfirmedWithY() {
        for (String notYet : new String[] {null, "", "N", "n"}) {
            rejected(copy(valid(), "confirm", notYet), InvalidNewTransactionException.class,
                    TransactionAddService.MSG_CONFIRM);
        }
        rejected(copy(valid(), "confirm", "X"), InvalidNewTransactionException.class,
                TransactionAddService.MSG_CONFIRM_INVALID);
        assertThat(transactions.count()).isEqualTo(300);
    }

    private static NewTransactionRequest with(UnaryOperator<NewTransactionRequest> change) {
        return change.apply(valid());
    }

    static NewTransactionRequest copy(NewTransactionRequest r, String field, String value) {
        return new NewTransactionRequest(
                field.equals("accountId") ? value : r.accountId(),
                field.equals("cardNumber") ? value : r.cardNumber(),
                field.equals("typeCode") ? value : r.typeCode(),
                field.equals("categoryCode") ? value : r.categoryCode(),
                field.equals("source") ? value : r.source(),
                field.equals("description") ? value : r.description(),
                field.equals("amount") ? value : r.amount(),
                field.equals("originDate") ? value : r.originDate(),
                field.equals("processedDate") ? value : r.processedDate(),
                field.equals("merchantId") ? value : r.merchantId(),
                field.equals("merchantName") ? value : r.merchantName(),
                field.equals("merchantCity") ? value : r.merchantCity(),
                field.equals("merchantZip") ? value : r.merchantZip(),
                field.equals("confirm") ? value : r.confirm());
    }

    @Test
    void leadingSpacesInTextFieldsAreKeptAndNumbersAreTrimmed() {
        var request = copy(copy(valid(), "description", "  Refund  "), "amount", " +00000001.00 ");
        String id = service.addTransaction(request).transactionId();
        var saved = transactions.findById(id).orElseThrow();
        assertThat(saved.getDescription()).isEqualTo("  Refund");
        assertThat(saved.getAmount()).isEqualByComparingTo("1.00");
    }

    @Test
    void refusesToAddOnceTheHighest16DigitIdIsTaken() {
        var full = new Transaction();
        full.setId("9999999999999999");
        entityManager.persist(full);
        rejected(valid(), TransactionIdsExhaustedException.class, TransactionAddService.MSG_UNABLE_TO_ADD);
    }
}
