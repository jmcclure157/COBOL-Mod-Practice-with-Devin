package com.carddemo.service;

/** Input failed the COTRN00C PROCESS-ENTER-KEY checks (or the page-number check added for REST). */
public class InvalidTransactionListRequestException extends RuntimeException {

    public InvalidTransactionListRequestException(String message) {
        super(message);
    }
}
