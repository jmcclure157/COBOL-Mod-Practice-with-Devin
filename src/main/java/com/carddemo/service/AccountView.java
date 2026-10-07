package com.carddemo.service;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Everything the COACTVW (account view) 3270 screen shows, as one JSON-friendly object. */
public record AccountView(
        Long accountId,
        String activeStatus,
        LocalDate openDate,
        LocalDate expirationDate,
        LocalDate reissueDate,
        BigDecimal creditLimit,
        BigDecimal cashCreditLimit,
        BigDecimal currentBalance,
        BigDecimal currentCycleCredit,
        BigDecimal currentCycleDebit,
        String groupId,
        CustomerView customer) {

    public record CustomerView(
            Long customerId,
            String ssn,
            LocalDate dateOfBirth,
            Integer ficoScore,
            String firstName,
            String middleName,
            String lastName,
            String addressLine1,
            String addressLine2,
            String city,
            String state,
            String zip,
            String country,
            String phone1,
            String phone2,
            String governmentId,
            String eftAccountId,
            String primaryCardHolder) {
    }
}
