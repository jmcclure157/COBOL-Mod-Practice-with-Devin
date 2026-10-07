package com.carddemo.service;

import java.math.BigDecimal;

/** What COBIL00C reports after paying: the payment transaction, the amount paid and the balance left (zero). */
public record BillPaymentResult(String transactionId, BigDecimal amountPaid, BigDecimal newBalance, String message) {
}
