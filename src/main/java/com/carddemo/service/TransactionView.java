package com.carddemo.service;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Everything the COTRN01 (view transaction) 3270 screen shows. Like the screen, timestamps are shown as dates. */
public record TransactionView(
        String transactionId,
        String cardNumber,
        String typeCode,
        Integer categoryCode,
        String source,
        BigDecimal amount,
        String description,
        LocalDate originDate,
        LocalDate processedDate,
        Long merchantId,
        String merchantName,
        String merchantCity,
        String merchantZip) {
}
