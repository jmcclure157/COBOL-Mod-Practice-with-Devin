package com.carddemo.model;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.math.BigDecimal;

/** Running balance per account + transaction type/category. Source: copybook CVTRA01Y (TRAN-CAT-BAL-RECORD, 50 bytes), VSAM file TCATBALF. */
@Entity
@Table(name = "transaction_category_balance")
public class TransactionCategoryBalance {

    @EmbeddedId
    private TransactionCategoryBalanceId id;

    /** TRAN-CAT-BAL PIC S9(09)V99. */
    @Column(precision = 11, scale = 2)
    private BigDecimal balance;

    public TransactionCategoryBalanceId getId() { return id; }
    public void setId(TransactionCategoryBalanceId id) { this.id = id; }
    public BigDecimal getBalance() { return balance; }
    public void setBalance(BigDecimal balance) { this.balance = balance; }
}
