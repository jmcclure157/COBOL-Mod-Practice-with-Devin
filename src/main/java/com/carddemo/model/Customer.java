package com.carddemo.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDate;

/** Customer master record. Source: copybook CVCUS01Y (CUSTOMER-RECORD, 500 bytes), VSAM file CUSTDAT. */
@Entity
@Table(name = "customer")
public class Customer {

    /** CUST-ID PIC 9(09) - VSAM key. */
    @Id
    private Long id;

    /** CUST-FIRST-NAME PIC X(25). */
    @Column(length = 25)
    private String firstName;

    /** CUST-MIDDLE-NAME PIC X(25). */
    @Column(length = 25)
    private String middleName;

    /** CUST-LAST-NAME PIC X(25). */
    @Column(length = 25)
    private String lastName;

    /** CUST-ADDR-LINE-1 PIC X(50). */
    @Column(length = 50)
    private String addressLine1;

    /** CUST-ADDR-LINE-2 PIC X(50). */
    @Column(length = 50)
    private String addressLine2;

    /** CUST-ADDR-LINE-3 PIC X(50) - shown as "City" on the account view screen. */
    @Column(length = 50)
    private String addressLine3;

    /** CUST-ADDR-STATE-CD PIC X(02). */
    @Column(length = 2)
    private String stateCode;

    /** CUST-ADDR-COUNTRY-CD PIC X(03). */
    @Column(length = 3)
    private String countryCode;

    /** CUST-ADDR-ZIP PIC X(10). */
    @Column(length = 10)
    private String zip;

    /** CUST-PHONE-NUM-1 PIC X(15). */
    @Column(length = 15)
    private String phone1;

    /** CUST-PHONE-NUM-2 PIC X(15). */
    @Column(length = 15)
    private String phone2;

    /** CUST-SSN PIC 9(09) - kept as text so leading zeros survive. */
    @Column(length = 9)
    private String ssn;

    /** CUST-GOVT-ISSUED-ID PIC X(20). */
    @Column(length = 20)
    private String governmentId;

    /** CUST-DOB-YYYY-MM-DD PIC X(10). */
    private LocalDate dateOfBirth;

    /** CUST-EFT-ACCOUNT-ID PIC X(10) - bank account for electronic payments. */
    @Column(length = 10)
    private String eftAccountId;

    /** CUST-PRI-CARD-HOLDER-IND PIC X(01) - 'Y' if primary card holder. */
    @Column(length = 1)
    private String primaryCardHolder;

    /** CUST-FICO-CREDIT-SCORE PIC 9(03). */
    private Integer ficoScore;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    public String getMiddleName() { return middleName; }
    public void setMiddleName(String middleName) { this.middleName = middleName; }
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    public String getAddressLine1() { return addressLine1; }
    public void setAddressLine1(String addressLine1) { this.addressLine1 = addressLine1; }
    public String getAddressLine2() { return addressLine2; }
    public void setAddressLine2(String addressLine2) { this.addressLine2 = addressLine2; }
    public String getAddressLine3() { return addressLine3; }
    public void setAddressLine3(String addressLine3) { this.addressLine3 = addressLine3; }
    public String getStateCode() { return stateCode; }
    public void setStateCode(String stateCode) { this.stateCode = stateCode; }
    public String getCountryCode() { return countryCode; }
    public void setCountryCode(String countryCode) { this.countryCode = countryCode; }
    public String getZip() { return zip; }
    public void setZip(String zip) { this.zip = zip; }
    public String getPhone1() { return phone1; }
    public void setPhone1(String phone1) { this.phone1 = phone1; }
    public String getPhone2() { return phone2; }
    public void setPhone2(String phone2) { this.phone2 = phone2; }
    public String getSsn() { return ssn; }
    public void setSsn(String ssn) { this.ssn = ssn; }
    public String getGovernmentId() { return governmentId; }
    public void setGovernmentId(String governmentId) { this.governmentId = governmentId; }
    public LocalDate getDateOfBirth() { return dateOfBirth; }
    public void setDateOfBirth(LocalDate dateOfBirth) { this.dateOfBirth = dateOfBirth; }
    public String getEftAccountId() { return eftAccountId; }
    public void setEftAccountId(String eftAccountId) { this.eftAccountId = eftAccountId; }
    public String getPrimaryCardHolder() { return primaryCardHolder; }
    public void setPrimaryCardHolder(String primaryCardHolder) { this.primaryCardHolder = primaryCardHolder; }
    public Integer getFicoScore() { return ficoScore; }
    public void setFicoScore(Integer ficoScore) { this.ficoScore = ficoScore; }
}
