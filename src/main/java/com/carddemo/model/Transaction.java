package com.carddemo.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;

/**
 * Posted transaction. Source: copybook CVTRA05Y (TRAN-RECORD, 350 bytes), VSAM file TRANSACT.
 * The daily input file DALYTRAN (copybook CVTRA06Y) has the same layout and feeds the CBTRN02C posting job.
 * Stub: not seeded yet (the TRANSACT file starts empty; POSTTRAN fills it).
 */
@Entity
@Table(name = "transactions")
public class Transaction {

    /** TRAN-ID PIC X(16) - VSAM key. */
    @Id
    @Column(length = 16)
    private String id;

    /** TRAN-TYPE-CD PIC X(02). */
    @Column(length = 2)
    private String typeCode;

    /** TRAN-CAT-CD PIC 9(04). */
    private Integer categoryCode;

    /** TRAN-SOURCE PIC X(10). */
    @Column(length = 10)
    private String source;

    /** TRAN-DESC PIC X(100). */
    @Column(length = 100)
    private String description;

    /** TRAN-AMT PIC S9(09)V99. */
    @Column(precision = 11, scale = 2)
    private BigDecimal amount;

    /** TRAN-MERCHANT-ID PIC 9(09). */
    private Long merchantId;

    /** TRAN-MERCHANT-NAME PIC X(50). */
    @Column(length = 50)
    private String merchantName;

    /** TRAN-MERCHANT-CITY PIC X(50). */
    @Column(length = 50)
    private String merchantCity;

    /** TRAN-MERCHANT-ZIP PIC X(10). */
    @Column(length = 10)
    private String merchantZip;

    /** TRAN-CARD-NUM PIC X(16). */
    @Column(length = 16)
    private String cardNumber;

    /** TRAN-ORIG-TS PIC X(26) - DB2-style timestamp text, e.g. 2022-06-10 19:27:53.000000. */
    @Column(length = 26)
    private String originTimestamp;

    /** TRAN-PROC-TS PIC X(26). */
    @Column(length = 26)
    private String processedTimestamp;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getTypeCode() { return typeCode; }
    public void setTypeCode(String typeCode) { this.typeCode = typeCode; }
    public Integer getCategoryCode() { return categoryCode; }
    public void setCategoryCode(Integer categoryCode) { this.categoryCode = categoryCode; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public Long getMerchantId() { return merchantId; }
    public void setMerchantId(Long merchantId) { this.merchantId = merchantId; }
    public String getMerchantName() { return merchantName; }
    public void setMerchantName(String merchantName) { this.merchantName = merchantName; }
    public String getMerchantCity() { return merchantCity; }
    public void setMerchantCity(String merchantCity) { this.merchantCity = merchantCity; }
    public String getMerchantZip() { return merchantZip; }
    public void setMerchantZip(String merchantZip) { this.merchantZip = merchantZip; }
    public String getCardNumber() { return cardNumber; }
    public void setCardNumber(String cardNumber) { this.cardNumber = cardNumber; }
    public String getOriginTimestamp() { return originTimestamp; }
    public void setOriginTimestamp(String originTimestamp) { this.originTimestamp = originTimestamp; }
    public String getProcessedTimestamp() { return processedTimestamp; }
    public void setProcessedTimestamp(String processedTimestamp) { this.processedTimestamp = processedTimestamp; }
}
