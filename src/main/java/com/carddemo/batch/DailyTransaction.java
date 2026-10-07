package com.carddemo.batch;

import com.carddemo.model.Transaction;

/** One DALYTRAN record: the raw 350-byte line (copied to the rejects file as-is) and its parsed fields. */
public record DailyTransaction(String record, Transaction transaction) {
}
