package com.carddemo.model;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/** Transaction category lookup (e.g. 01/0001 Regular Sales Draft). Source: copybook CVTRA04Y (TRAN-CAT-RECORD, 60 bytes), VSAM file TRANCATG. */
@Entity
@Table(name = "transaction_category")
public class TransactionCategory {

    @EmbeddedId
    private TransactionCategoryId id;

    /** TRAN-CAT-TYPE-DESC PIC X(50). */
    @Column(length = 50)
    private String description;

    public TransactionCategoryId getId() { return id; }
    public void setId(TransactionCategoryId id) { this.id = id; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
