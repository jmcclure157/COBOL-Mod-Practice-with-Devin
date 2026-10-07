package com.carddemo.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDate;

/** Credit card record. Source: copybook CVACT02Y (CARD-RECORD, 150 bytes), VSAM file CARDDAT. */
@Entity
@Table(name = "card")
public class Card {

    /** CARD-NUM PIC X(16) - VSAM key. */
    @Id
    @Column(length = 16)
    private String cardNumber;

    /** CARD-ACCT-ID PIC 9(11) - alternate index CARDAIX. */
    private Long accountId;

    /** CARD-CVV-CD PIC 9(03). */
    private Integer cvv;

    /** CARD-EMBOSSED-NAME PIC X(50). */
    @Column(length = 50)
    private String embossedName;

    /** CARD-EXPIRAION-DATE PIC X(10). */
    private LocalDate expirationDate;

    /** CARD-ACTIVE-STATUS PIC X(01). */
    @Column(length = 1)
    private String activeStatus;

    public String getCardNumber() { return cardNumber; }
    public void setCardNumber(String cardNumber) { this.cardNumber = cardNumber; }
    public Long getAccountId() { return accountId; }
    public void setAccountId(Long accountId) { this.accountId = accountId; }
    public Integer getCvv() { return cvv; }
    public void setCvv(Integer cvv) { this.cvv = cvv; }
    public String getEmbossedName() { return embossedName; }
    public void setEmbossedName(String embossedName) { this.embossedName = embossedName; }
    public LocalDate getExpirationDate() { return expirationDate; }
    public void setExpirationDate(LocalDate expirationDate) { this.expirationDate = expirationDate; }
    public String getActiveStatus() { return activeStatus; }
    public void setActiveStatus(String activeStatus) { this.activeStatus = activeStatus; }
}
