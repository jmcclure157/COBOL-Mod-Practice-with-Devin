package com.carddemo.service;

/** The COTRN02C READ of CXACAIX (by account) or CCXREF (by card) came back NOTFND. */
public class CardOrAccountNotFoundException extends RuntimeException {

    public CardOrAccountNotFoundException(String message) {
        super(message);
    }
}
