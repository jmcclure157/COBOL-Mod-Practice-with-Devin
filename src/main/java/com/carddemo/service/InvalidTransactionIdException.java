package com.carddemo.service;

/** The transaction ID failed the COTRN01C PROCESS-ENTER-KEY check (it was blank). */
public class InvalidTransactionIdException extends RuntimeException {

    public InvalidTransactionIdException(String message) {
        super(message);
    }
}
