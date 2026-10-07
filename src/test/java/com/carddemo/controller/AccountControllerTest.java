package com.carddemo.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** End-to-end: HTTP request -> controller -> service -> H2 seeded from the original CardDemo data files. */
@SpringBootTest
@AutoConfigureMockMvc
class AccountControllerTest {

    @Autowired MockMvc mvc;

    @Test
    void returnsSeededAccountWithItsCustomer() throws Exception {
        mvc.perform(get("/accounts/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountId").value(1))
                .andExpect(jsonPath("$.activeStatus").value("Y"))
                .andExpect(jsonPath("$.currentBalance").value(1288.10))
                .andExpect(jsonPath("$.creditLimit").value(2020.00))
                .andExpect(jsonPath("$.openDate").value("2014-11-20"))
                .andExpect(jsonPath("$.customer.customerId").value(1))
                .andExpect(jsonPath("$.customer.firstName").value("Immanuel"))
                .andExpect(jsonPath("$.customer.lastName").value("Kessler"))
                .andExpect(jsonPath("$.customer.ssn").value("020-97-3888"))
                .andExpect(jsonPath("$.customer.ficoScore").value(274));
    }

    @Test
    void acceptsZeroPaddedElevenDigitId() throws Exception {
        mvc.perform(get("/accounts/00000000001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountId").value(1));
    }

    @Test
    void unknownAccountIs404WithCobolMessage() throws Exception {
        mvc.perform(get("/accounts/99999999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Account:99999999999 not found in Cross ref file."));
    }

    @Test
    void invalidAccountIdIs400WithCobolMessage() throws Exception {
        mvc.perform(get("/accounts/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Account Filter must  be a non-zero 11 digit number"));
    }
}
