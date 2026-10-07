package com.carddemo.batch;

import java.util.Locale;

/** CBTRN02C WS-VALIDATION-TRAILER: why a daily transaction was not posted. */
public record PostingReject(int code, String description) {

    static final PostingReject INVALID_CARD = new PostingReject(100, "INVALID CARD NUMBER FOUND");
    static final PostingReject ACCOUNT_NOT_FOUND = new PostingReject(101, "ACCOUNT RECORD NOT FOUND");
    static final PostingReject OVERLIMIT = new PostingReject(102, "OVERLIMIT TRANSACTION");
    static final PostingReject EXPIRED = new PostingReject(103, "TRANSACTION RECEIVED AFTER ACCT EXPIRATION");

    /** WS-VALIDATION-FAIL-REASON PIC 9(04) + WS-VALIDATION-FAIL-REASON-DESC PIC X(76). */
    String trailer() {
        return String.format(Locale.ROOT, "%04d%-76.76s", code, description);
    }
}
