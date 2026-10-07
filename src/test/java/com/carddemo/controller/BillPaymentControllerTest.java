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

/** POST /accounts/{id}/payments end to end; each test rolls back so account balances stay as seeded. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class BillPaymentControllerTest {

    @Autowired MockMvc mvc;

    @Test
    void confirmedPaymentReturns201AndZeroesTheBalance() throws Exception {
        mvc.perform(post("/accounts/2/payments").contentType(MediaType.APPLICATION_JSON).content("{\"confirm\":\"Y\"}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/transactions/0000000996722788"))
                .andExpect(jsonPath("$.transactionId").value("0000000996722788"))
                .andExpect(jsonPath("$.amountPaid").value(158.00))
                .andExpect(jsonPath("$.newBalance").value(0))
                .andExpect(jsonPath("$.message").value("Payment successful.  Your Transaction ID is 0000000996722788."));

        mvc.perform(get("/accounts/2")).andExpect(jsonPath("$.currentBalance").value(0));
        mvc.perform(get("/transactions/0000000996722788"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.description").value("BILL PAYMENT - ONLINE"))
                .andExpect(jsonPath("$.amount").value(158.00));
    }

    @Test
    void missingBodyAsksToConfirm() throws Exception {
        mvc.perform(post("/accounts/2/payments"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Confirm to make a bill payment..."));
    }

    @Test
    void invalidConfirmIs400() throws Exception {
        mvc.perform(post("/accounts/2/payments").contentType(MediaType.APPLICATION_JSON).content("{\"confirm\":\"X\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Invalid value. Valid values are (Y/N)..."));
    }

    @Test
    void unknownAccountIs404() throws Exception {
        mvc.perform(post("/accounts/99999999999/payments").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"confirm\":\"Y\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Account ID NOT found..."));
    }

    @Test
    void secondPaymentHasNothingToPay() throws Exception {
        String body = "{\"confirm\":\"Y\"}";
        mvc.perform(post("/accounts/2/payments").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());
        mvc.perform(post("/accounts/2/payments").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("You have nothing to pay..."));
    }
}
