package com.carddemo.service;

/** Input failed the COACTVWC 2210-EDIT-ACCOUNT checks. */
public class InvalidAccountIdException extends RuntimeException {

    public InvalidAccountIdException(String message) {
        super(message);
    }
}
