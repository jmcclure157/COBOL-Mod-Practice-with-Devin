package com.carddemo.controller;

import com.carddemo.repository.AccountRepository;
import com.carddemo.repository.TransactionRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * No test-managed transaction here, so the payment really commits: both the new transaction and the
 * zeroed balance must be in the database afterwards. Cleans up so other tests still see the seeded data.
 */
@SpringBootTest
@AutoConfigureMockMvc
class BillPaymentCommitTest {

    @Autowired MockMvc mvc;
    @Autowired AccountRepository accounts;
    @Autowired TransactionRepository transactions;
    @Autowired TransactionTemplate tx;

    @AfterEach
    void restoreSeededData() {
        tx.executeWithoutResult(s -> {
            transactions.deleteById("0000000996722788");
            accounts.findById(4L).orElseThrow().setCurrentBalance(seededBalance);
        });
    }

    BigDecimal seededBalance;

    @Test
    void paymentCommitsTheTransactionAndTheBalanceTogether() throws Exception {
        seededBalance = accounts.findById(4L).orElseThrow().getCurrentBalance();
        assertThat(seededBalance.signum()).isPositive();

        mvc.perform(post("/accounts/4/payments").contentType(MediaType.APPLICATION_JSON).content("{\"confirm\":\"Y\"}"))
                .andExpect(status().isCreated());

        assertThat(accounts.findById(4L).orElseThrow().getCurrentBalance()).isEqualByComparingTo("0.00");
        assertThat(transactions.findById("0000000996722788").orElseThrow().getAmount())
                .isEqualByComparingTo(seededBalance);
    }
}
