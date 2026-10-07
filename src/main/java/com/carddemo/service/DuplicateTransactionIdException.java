package com.carddemo.service;

/** The COTRN02C WRITE to TRANSACT came back DUPKEY/DUPREC. */
public class DuplicateTransactionIdException extends RuntimeException {

    public DuplicateTransactionIdException(String message) {
        super(message);
    }
}
