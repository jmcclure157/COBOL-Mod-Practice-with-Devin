package com.carddemo.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Runs against the 300 transactions seeded from dailytran.txt. */
@SpringBootTest
class TransactionViewServiceTest {

    @Autowired TransactionViewService service;

    @Test
    void showsEveryFieldOfTheScreen() {
        var view = service.viewTransaction("0000000000683580");
        assertThat(view.transactionId()).isEqualTo("0000000000683580");
        assertThat(view.cardNumber()).isEqualTo("4859452612877065");
        assertThat(view.typeCode()).isEqualTo("01");
        assertThat(view.categoryCode()).isEqualTo(1);
        assertThat(view.source()).isEqualTo("POS TERM");
        assertThat(view.amount()).isEqualByComparingTo("504.77");
        assertThat(view.description()).isEqualTo("Purchase at Abshire-Lowe");
        assertThat(view.originDate()).hasToString("2022-06-10");
        assertThat(view.processedDate()).isNull();
        assertThat(view.merchantId()).isEqualTo(800000000L);
        assertThat(view.merchantName()).isEqualTo("Abshire-Lowe");
        assertThat(view.merchantCity()).isEqualTo("North Enoshaven");
        assertThat(view.merchantZip()).isEqualTo("72112");
    }

    @Test
    void showsNegativeAmounts() {
        assertThat(service.viewTransaction("0000000001774260").amount()).isEqualByComparingTo("-919.00");
    }

    @Test
    void blankIdIsRejected() {
        for (String blank : new String[] {null, "", "   "}) {
            assertThatThrownBy(() -> service.viewTransaction(blank))
                    .isInstanceOf(InvalidTransactionIdException.class)
                    .hasMessage(TransactionViewService.MSG_TRAN_ID_EMPTY);
        }
    }

    @Test
    void unknownOrUnpaddedIdIsNotFound() {
        for (String missing : new String[] {"0000000000000001", "683580", "abc"}) {
            assertThatThrownBy(() -> service.viewTransaction(missing))
                    .isInstanceOf(TransactionNotFoundException.class)
                    .hasMessage(TransactionViewService.MSG_TRAN_ID_NOT_FOUND);
        }
    }
}
