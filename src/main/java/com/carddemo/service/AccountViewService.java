package com.carddemo.service;

import com.carddemo.model.Account;
import com.carddemo.model.CardXref;
import com.carddemo.model.Customer;
import com.carddemo.repository.AccountRepository;
import com.carddemo.repository.CardXrefRepository;
import com.carddemo.repository.CustomerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Business logic of CICS program COACTVWC (transaction CAVW, "Account View"), without the screen handling.
 * Paragraph names in the comments point back to the COBOL source.
 */
@Service
public class AccountViewService {

    static final String MSG_ACCOUNT_NOT_PROVIDED = "Account number not provided";
    static final String MSG_ACCOUNT_INVALID = "Account Filter must  be a non-zero 11 digit number";

    private final CardXrefRepository cardXrefs;
    private final AccountRepository accounts;
    private final CustomerRepository customers;

    public AccountViewService(CardXrefRepository cardXrefs, AccountRepository accounts, CustomerRepository customers) {
        this.cardXrefs = cardXrefs;
        this.accounts = accounts;
        this.customers = customers;
    }

    @Transactional(readOnly = true)
    public AccountView viewAccount(String rawAccountId) {
        long accountId = editAccount(rawAccountId);

        // 9200-GETCARDXREF-BYACCT: READ CXACAIX (cross-reference by account id) to find the customer.
        CardXref xref = cardXrefs.findFirstByAccountIdOrderByCardNumberAsc(accountId)
                .orElseThrow(() -> new AccountNotFoundException(
                        "Account:%011d not found in Cross ref file.".formatted(accountId)));

        // 9300-GETACCTDATA-BYACCT: READ ACCTDAT.
        Account account = accounts.findById(accountId)
                .orElseThrow(() -> new AccountNotFoundException(
                        "Account:%011d not found in Acct Master file.".formatted(accountId)));

        // 9400-GETCUSTDATA-BYCUST: READ CUSTDAT.
        Customer customer = customers.findById(xref.getCustomerId())
                .orElseThrow(() -> new AccountNotFoundException(
                        "CustId:%09d not found in customer master.".formatted(xref.getCustomerId())));

        return toView(account, customer);
    }

    /**
     * 2210-EDIT-ACCOUNT: blank or '*' means "not provided"; otherwise it must be numeric, non-zero and fit
     * in ACCT-ID PIC 9(11). Unlike the 3270 field, shorter values such as "1" are accepted.
     */
    private long editAccount(String rawAccountId) {
        String value = rawAccountId == null ? "" : rawAccountId.strip();
        if (value.isEmpty() || value.equals("*")) {
            throw new InvalidAccountIdException(MSG_ACCOUNT_NOT_PROVIDED);
        }
        if (value.length() > 11 || !value.chars().allMatch(ch -> ch >= '0' && ch <= '9')) {
            throw new InvalidAccountIdException(MSG_ACCOUNT_INVALID);
        }
        long accountId = Long.parseLong(value);
        if (accountId == 0) {
            throw new InvalidAccountIdException(MSG_ACCOUNT_INVALID);
        }
        return accountId;
    }

    /** 1200-SETUP-SCREEN-VARS: copy record fields onto the screen. */
    private static AccountView toView(Account a, Customer c) {
        return new AccountView(
                a.getId(),
                a.getActiveStatus(),
                a.getOpenDate(),
                a.getExpirationDate(),
                a.getReissueDate(),
                a.getCreditLimit(),
                a.getCashCreditLimit(),
                a.getCurrentBalance(),
                a.getCurrentCycleCredit(),
                a.getCurrentCycleDebit(),
                a.getGroupId(),
                new AccountView.CustomerView(
                        c.getId(),
                        formatSsn(c.getSsn()),
                        c.getDateOfBirth(),
                        c.getFicoScore(),
                        c.getFirstName(),
                        c.getMiddleName(),
                        c.getLastName(),
                        c.getAddressLine1(),
                        c.getAddressLine2(),
                        c.getAddressLine3(),
                        c.getStateCode(),
                        c.getZip(),
                        c.getCountryCode(),
                        c.getPhone1(),
                        c.getPhone2(),
                        c.getGovernmentId(),
                        c.getEftAccountId(),
                        c.getPrimaryCardHolder()));
    }

    /** STRING CUST-SSN(1:3) '-' CUST-SSN(4:2) '-' CUST-SSN(6:4). */
    private static String formatSsn(String ssn) {
        return ssn.substring(0, 3) + "-" + ssn.substring(3, 5) + "-" + ssn.substring(5, 9);
    }
}
