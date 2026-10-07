package com.carddemo.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Sign-on user. Source: copybook CSUSR01Y (SEC-USER-DATA, 80 bytes), VSAM file USRSEC.
 * Stub: not seeded (the sample file ships only in EBCDIC). The COBOL app stores passwords in plain text;
 * the Java version must hash them (e.g. Spring Security BCrypt) when COSGN00C is migrated.
 */
@Entity
@Table(name = "user_security")
public class UserSecurity {

    /** SEC-USR-ID PIC X(08) - VSAM key. */
    @Id
    @Column(length = 8)
    private String userId;

    /** SEC-USR-FNAME PIC X(20). */
    @Column(length = 20)
    private String firstName;

    /** SEC-USR-LNAME PIC X(20). */
    @Column(length = 20)
    private String lastName;

    /** SEC-USR-PWD PIC X(08). */
    private String password;

    /** SEC-USR-TYPE PIC X(01) - 'A' admin / 'U' regular user. */
    @Column(length = 1)
    private String userType;

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getUserType() { return userType; }
    public void setUserType(String userType) { this.userType = userType; }
}
