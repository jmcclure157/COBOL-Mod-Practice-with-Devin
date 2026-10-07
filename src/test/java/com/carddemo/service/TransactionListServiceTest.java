package com.carddemo.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Runs against the 300 transactions seeded from dailytran.txt. */
@SpringBootTest
class TransactionListServiceTest {

    @Autowired TransactionListService service;

    @Test
    void firstPageHasTenRowsInIdOrderAndANextPage() {
        var page = service.listTransactions(null, null);
        assertThat(page.page()).isEqualTo(1);
        assertThat(page.hasNextPage()).isTrue();
        assertThat(page.transactions()).hasSize(10);
        assertThat(page.transactions().get(0).transactionId()).isEqualTo("0000000000683580");
        assertThat(page.transactions().get(9).transactionId()).isEqualTo("0000000021711604");
    }

    @Test
    void secondPageContinuesWhereTheFirstStopped() {
        var page = service.listTransactions(null, "2");
        assertThat(page.transactions().get(0).transactionId()).isEqualTo("0000000025430891");
    }

    @Test
    void lastPageHasNoNextPageAndPastTheEndIsEmpty() {
        var last = service.listTransactions(null, "27");
        assertThat(last.hasNextPage()).isFalse();
        assertThat(last.transactions()).hasSize(2);
        assertThat(last.transactions().get(1).transactionId()).isEqualTo("0000000996722787");

        var pastEnd = service.listTransactions(null, "28");
        assertThat(pastEnd.hasNextPage()).isFalse();
        assertThat(pastEnd.transactions()).isEmpty();
    }

    @Test
    void startIdBeginsTheBrowseAtThatKeyOrTheNextHigherOne() {
        assertThat(service.listTransactions("0000000025430891", null).transactions().get(0).transactionId())
                .isEqualTo("0000000025430891");
        assertThat(service.listTransactions("25430891", null).transactions().get(0).transactionId())
                .isEqualTo("0000000025430891");
        assertThat(service.listTransactions("25430890", null).transactions().get(0).transactionId())
                .isEqualTo("0000000025430891");
        assertThat(service.listTransactions("9999999999999999", null).transactions()).isEmpty();
    }

    @Test
    void blankStartIdMeansFromTheTop() {
        assertThat(service.listTransactions("  ", null).transactions().get(0).transactionId())
                .isEqualTo("0000000000683580");
    }

    @Test
    void rowShowsDateFromTheOriginTimestamp() {
        var row = service.listTransactions(null, null).transactions().get(0);
        assertThat(row.date()).hasToString("2022-06-10");
        assertThat(row.description()).isEqualTo("Purchase at Abshire-Lowe");
        assertThat(row.amount()).isEqualByComparingTo("504.77");
    }

    @Test
    void rejectsNonNumericOrTooLongStartId() {
        for (String bad : new String[] {"abc", "12a", "-1", "1.5", "12345678901234567"}) {
            assertThatThrownBy(() -> service.listTransactions(bad, null))
                    .isInstanceOf(InvalidTransactionListRequestException.class)
                    .hasMessage(TransactionListService.MSG_TRAN_ID_NOT_NUMERIC);
        }
    }

    @Test
    void rejectsBadPageNumbers() {
        for (String bad : new String[] {"0", "-1", "abc", "1.5", "9999999999"}) {
            assertThatThrownBy(() -> service.listTransactions(null, bad))
                    .isInstanceOf(InvalidTransactionListRequestException.class)
                    .hasMessage(TransactionListService.MSG_PAGE_INVALID);
        }
    }

    @Test
    void rejectsPageNumbersWhoseRowOffsetWouldOverflowAnInt() {
        assertThat(service.listTransactions(null, "214748365").transactions()).isEmpty();
        assertThatThrownBy(() -> service.listTransactions(null, "214748366"))
                .isInstanceOf(InvalidTransactionListRequestException.class)
                .hasMessage(TransactionListService.MSG_PAGE_INVALID);
    }
}
