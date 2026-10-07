package com.carddemo.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Card / customer / account cross-reference. Source: copybook CVACT03Y (CARD-XREF-RECORD, 50 bytes),
 * VSAM file CARDXREF with alternate index CXACAIX on the account id.
 */
@Entity
@Table(name = "card_xref")
public class CardXref {

    /** XREF-CARD-NUM PIC X(16) - VSAM key. */
    @Id
    @Column(length = 16)
    private String cardNumber;

    /** XREF-CUST-ID PIC 9(09). */
    private Long customerId;

    /** XREF-ACCT-ID PIC 9(11) - alternate index CXACAIX. */
    private Long accountId;

    public String getCardNumber() { return cardNumber; }
    public void setCardNumber(String cardNumber) { this.cardNumber = cardNumber; }
    public Long getCustomerId() { return customerId; }
    public void setCustomerId(Long customerId) { this.customerId = customerId; }
    public Long getAccountId() { return accountId; }
    public void setAccountId(Long accountId) { this.accountId = accountId; }
}
