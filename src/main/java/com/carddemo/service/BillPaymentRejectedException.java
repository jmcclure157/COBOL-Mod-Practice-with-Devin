package com.carddemo.service;

/** COBIL00C refused to pay: confirmation missing or invalid, or the balance is not above zero. */
public class BillPaymentRejectedException extends RuntimeException {

    public BillPaymentRejectedException(String message) {
        super(message);
    }
}
