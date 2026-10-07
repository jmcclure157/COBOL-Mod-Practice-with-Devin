package com.carddemo.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Transaction type lookup (e.g. 01 Purchase). Source: copybook CVTRA03Y (TRAN-TYPE-RECORD, 60 bytes), VSAM file TRANTYPE. */
@Entity
@Table(name = "transaction_type")
public class TransactionType {

    /** TRAN-TYPE PIC X(02) - VSAM key. */
    @Id
    @Column(length = 2)
    private String code;

    /** TRAN-TYPE-DESC PIC X(50). */
    @Column(length = 50)
    private String description;

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
