package com.carddemo.service;

/** The highest TRAN-ID is already 9999999999999999, so there is no next 16-digit id to give out. */
public class TransactionIdsExhaustedException extends RuntimeException {

    public TransactionIdsExhaustedException(String message) {
        super(message);
    }
}
