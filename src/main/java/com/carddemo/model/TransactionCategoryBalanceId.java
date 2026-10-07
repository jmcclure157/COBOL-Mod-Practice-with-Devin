package com.carddemo.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/** TRAN-CAT-KEY from copybook CVTRA01Y: TRANCAT-ACCT-ID 9(11) + TRANCAT-TYPE-CD X(02) + TRANCAT-CD 9(04). */
@Embeddable
public record TransactionCategoryBalanceId(
        Long accountId,
        @Column(length = 2) String typeCode,
        Integer categoryCode) {
}
