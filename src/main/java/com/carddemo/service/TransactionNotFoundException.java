package com.carddemo.service;

/** The COTRN01C READ of TRANSACT came back NOTFND. */
public class TransactionNotFoundException extends RuntimeException {

    public TransactionNotFoundException(String message) {
        super(message);
    }
}
