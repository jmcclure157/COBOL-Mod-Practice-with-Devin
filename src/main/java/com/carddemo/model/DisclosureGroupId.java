package com.carddemo.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/** DIS-GROUP-KEY from copybook CVTRA02Y: DIS-ACCT-GROUP-ID X(10) + DIS-TRAN-TYPE-CD X(02) + DIS-TRAN-CAT-CD 9(04). */
@Embeddable
public record DisclosureGroupId(
        @Column(length = 10) String groupId,
        @Column(length = 2) String typeCode,
        Integer categoryCode) {
}
