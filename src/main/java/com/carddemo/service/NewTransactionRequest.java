package com.carddemo.service;

/**
 * The input fields of the COTRN02 (add transaction) 3270 screen. Every field is text, as typed on the screen,
 * so the COBOL edits (and their messages) can be applied exactly.
 */
public record NewTransactionRequest(
        String accountId,
        String cardNumber,
        String typeCode,
        String categoryCode,
        String source,
        String description,
        String amount,
        String originDate,
        String processedDate,
        String merchantId,
        String merchantName,
        String merchantCity,
        String merchantZip,
        String confirm) {
}
