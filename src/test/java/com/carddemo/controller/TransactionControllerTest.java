package com.carddemo.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
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
    void leadingSpaceIsPartOfTheKeySoNotFound() throws Exception {
        mvc.perform(get("/transactions/%200000000000683580"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Transaction ID NOT found..."));
    }

    @Test
    void blankTransactionIdIs400WithCobolMessage() throws Exception {
        mvc.perform(get("/transactions/{id}", " "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Tran ID can NOT be empty..."));
    }

    private static final String NEW_TRANSACTION = """
            {"accountId": "50", "typeCode": "01", "categoryCode": "0001", "source": "POS TERM",
             "description": "Purchase at Test Store", "amount": "+00000123.45",
             "originDate": "2026-10-07", "processedDate": "2026-10-07", "merchantId": "800000000",
             "merchantName": "Test Store", "merchantCity": "Dallas", "merchantZip": "75201", "confirm": "Y"}
            """;

    @Test
    @Transactional
    void addTransactionIs201WithLocationAndCobolSuccessMessage() throws Exception {
        mvc.perform(post("/transactions").contentType(MediaType.APPLICATION_JSON).content(NEW_TRANSACTION))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/transactions/0000000996722788"))
                .andExpect(jsonPath("$.transactionId").value("0000000996722788"))
                .andExpect(jsonPath("$.message")
                        .value("Transaction added successfully.  Your Tran ID is 0000000996722788."));

        mvc.perform(get("/transactions/{id}", "0000000996722788"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cardNumber").value("0500024453765740"))
                .andExpect(jsonPath("$.amount").value(123.45));
    }

    @Test
    @Transactional
    void addTransactionValidationIs400AndUnknownAccountIs404() throws Exception {
        mvc.perform(post("/transactions").contentType(MediaType.APPLICATION_JSON)
                        .content(NEW_TRANSACTION.replace("\"+00000123.45\"", "\"123.45\"")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Amount should be in format -99999999.99"));

        mvc.perform(post("/transactions").contentType(MediaType.APPLICATION_JSON)
                        .content(NEW_TRANSACTION.replace("\"50\"", "\"99999\"")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Account ID NOT found..."));
    }
}
