package com.carddemo.model;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.math.BigDecimal;

/** Interest rate per account group + transaction type/category. Source: copybook CVTRA02Y (DIS-GROUP-RECORD, 50 bytes), VSAM file DISCGRP. */
@Entity
@Table(name = "disclosure_group")
public class DisclosureGroup {

    @EmbeddedId
    private DisclosureGroupId id;

    /** DIS-INT-RATE PIC S9(04)V99 - annual percentage rate. */
    @Column(precision = 6, scale = 2)
    private BigDecimal interestRate;

    public DisclosureGroupId getId() { return id; }
    public void setId(DisclosureGroupId id) { this.id = id; }
    public BigDecimal getInterestRate() { return interestRate; }
    public void setInterestRate(BigDecimal interestRate) { this.interestRate = interestRate; }
}
