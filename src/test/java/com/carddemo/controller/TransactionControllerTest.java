package com.carddemo.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** End-to-end: HTTP request -> controller -> service -> H2 seeded from dailytran.txt. */
@SpringBootTest
@AutoConfigureMockMvc
class TransactionControllerTest {

    @Autowired MockMvc mvc;

    @Test
    void firstPageOfTransactions() throws Exception {
        mvc.perform(get("/transactions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.hasNextPage").value(true))
                .andExpect(jsonPath("$.transactions.length()").value(10))
                .andExpect(jsonPath("$.transactions[0].transactionId").value("0000000000683580"))
                .andExpect(jsonPath("$.transactions[0].date").value("2022-06-10"))
                .andExpect(jsonPath("$.transactions[0].description").value("Purchase at Abshire-Lowe"))
                .andExpect(jsonPath("$.transactions[0].amount").value(504.77))
                .andExpect(jsonPath("$.transactions[1].amount").value(-919.00));
    }

    @Test
    void pageAndStartIdParameters() throws Exception {
        mvc.perform(get("/transactions").param("page", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(2))
                .andExpect(jsonPath("$.transactions[0].transactionId").value("0000000025430891"));

        mvc.perform(get("/transactions").param("startId", "25430891"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactions[0].transactionId").value("0000000025430891"));
    }

    @Test
    void nonNumericStartIdIs400WithCobolMessage() throws Exception {
        mvc.perform(get("/transactions").param("startId", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Tran ID must be Numeric ..."));
    }

    @Test
    void badPageIs400() throws Exception {
        mvc.perform(get("/transactions").param("page", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Page must be a positive number"));
    }

    @Test
    void viewOneTransaction() throws Exception {
        mvc.perform(get("/transactions/{id}", "0000000000683580"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactionId").value("0000000000683580"))
                .andExpect(jsonPath("$.cardNumber").value("4859452612877065"))
                .andExpect(jsonPath("$.amount").value(504.77))
                .andExpect(jsonPath("$.originDate").value("2022-06-10"))
                .andExpect(jsonPath("$.merchantName").value("Abshire-Lowe"));
    }

    @Test
    void unknownTransactionIs404WithCobolMessage() throws Exception {
        mvc.perform(get("/transactions/{id}", "0000000000000001"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Transaction ID NOT found..."));
    }

    @Test
    void blankTransactionIdIs400WithCobolMessage() throws Exception {
        mvc.perform(get("/transactions/{id}", " "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Tran ID can NOT be empty..."));
    }
}
