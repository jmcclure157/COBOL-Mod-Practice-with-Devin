package com.carddemo.service;

/** A COACTVWC file READ came back NOTFND (cross-reference, account master or customer master). */
public class AccountNotFoundException extends RuntimeException {

    public AccountNotFoundException(String message) {
        super(message);
    }
}
