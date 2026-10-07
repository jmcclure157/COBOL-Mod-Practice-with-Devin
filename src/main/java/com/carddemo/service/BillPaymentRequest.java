package com.carddemo.service;

/** The CONFIRM field of the COBIL00 (bill payment) screen; the account id comes from the URL. */
public record BillPaymentRequest(String confirm) {
}
