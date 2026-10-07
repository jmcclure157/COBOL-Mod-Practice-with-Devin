package com.carddemo.service;

/** What COTRN02C shows after a successful WRITE: the new id and its green confirmation message. */
public record NewTransactionResult(String transactionId, String message) {
}
