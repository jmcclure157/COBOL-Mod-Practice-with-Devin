package com.carddemo.service;

/** Input failed one of the COTRN02C edits (VALIDATE-INPUT-KEY-FIELDS, VALIDATE-INPUT-DATA-FIELDS or the confirm check). */
public class InvalidNewTransactionException extends RuntimeException {

    public InvalidNewTransactionException(String message) {
        super(message);
    }
}
