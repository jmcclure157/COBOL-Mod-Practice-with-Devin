package com.carddemo.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/** TRAN-CAT-KEY from copybook CVTRA04Y: TRAN-TYPE-CD PIC X(02) + TRAN-CAT-CD PIC 9(04). */
@Embeddable
public record TransactionCategoryId(
        @Column(length = 2) String typeCode,
        Integer categoryCode) {
}
