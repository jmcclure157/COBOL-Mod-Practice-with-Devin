package com.carddemo.seed;

import com.carddemo.model.Account;
import com.carddemo.model.Card;
import com.carddemo.model.CardXref;
import com.carddemo.model.Customer;
import com.carddemo.model.DisclosureGroup;
import com.carddemo.model.DisclosureGroupId;
import com.carddemo.model.TransactionCategory;
import com.carddemo.model.TransactionCategoryBalance;
import com.carddemo.model.TransactionCategoryBalanceId;
import com.carddemo.model.TransactionCategoryId;
import com.carddemo.model.TransactionType;
import com.carddemo.repository.AccountRepository;
import com.carddemo.repository.CardRepository;
import com.carddemo.repository.CardXrefRepository;
import com.carddemo.repository.CustomerRepository;
import com.carddemo.repository.DisclosureGroupRepository;
import com.carddemo.repository.TransactionCategoryBalanceRepository;
import com.carddemo.repository.TransactionCategoryRepository;
import com.carddemo.repository.TransactionTypeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ResourceLoader;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.function.Function;

/**
 * Seeds the database from the original CardDemo ASCII sample files (carddemo/app/data/ASCII),
 * the same files the IDCAMS REPRO jobs (ACCTFILE, CUSTFILE, CARDFILE, XREFFILE, ...) load into VSAM.
 * Each parse method follows its copybook field by field.
 */
@Component
@ConditionalOnProperty(name = "carddemo.seed.enabled", havingValue = "true", matchIfMissing = true)
public class CardDemoDataLoader implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(CardDemoDataLoader.class);

    private final ResourceLoader resourceLoader;
    private final String location;
    private final AccountRepository accounts;
    private final CustomerRepository customers;
    private final CardRepository cards;
    private final CardXrefRepository cardXrefs;
    private final TransactionTypeRepository transactionTypes;
    private final TransactionCategoryRepository transactionCategories;
    private final DisclosureGroupRepository disclosureGroups;
    private final TransactionCategoryBalanceRepository categoryBalances;

    public CardDemoDataLoader(ResourceLoader resourceLoader,
                              @Value("${carddemo.seed.location:classpath:carddemo-data/}") String location,
                              AccountRepository accounts,
                              CustomerRepository customers,
                              CardRepository cards,
                              CardXrefRepository cardXrefs,
                              TransactionTypeRepository transactionTypes,
                              TransactionCategoryRepository transactionCategories,
                              DisclosureGroupRepository disclosureGroups,
                              TransactionCategoryBalanceRepository categoryBalances) {
        this.resourceLoader = resourceLoader;
        this.location = location;
        this.accounts = accounts;
        this.customers = customers;
        this.cards = cards;
        this.cardXrefs = cardXrefs;
        this.transactionTypes = transactionTypes;
        this.transactionCategories = transactionCategories;
        this.disclosureGroups = disclosureGroups;
        this.categoryBalances = categoryBalances;
    }

    @Override
    public void run(ApplicationArguments args) {
        load("acctdata.txt", accounts, CardDemoDataLoader::parseAccount);
        load("custdata.txt", customers, CardDemoDataLoader::parseCustomer);
        load("carddata.txt", cards, CardDemoDataLoader::parseCard);
        load("cardxref.txt", cardXrefs, CardDemoDataLoader::parseCardXref);
        load("trantype.txt", transactionTypes, CardDemoDataLoader::parseTransactionType);
        load("trancatg.txt", transactionCategories, CardDemoDataLoader::parseTransactionCategory);
        load("discgrp.txt", disclosureGroups, CardDemoDataLoader::parseDisclosureGroup);
        load("tcatbal.txt", categoryBalances, CardDemoDataLoader::parseCategoryBalance);
    }

    private <T> void load(String fileName, JpaRepository<T, ?> repository, Function<String, T> parser) {
        if (repository.count() > 0) {
            return;
        }
        List<T> records = readLines(fileName).stream().map(parser).toList();
        repository.saveAll(records);
        log.info("Seeded {} records from {}", records.size(), fileName);
    }

    private List<String> readLines(String fileName) {
        var resource = resourceLoader.getResource(location + fileName);
        try (var reader = new BufferedReader(new InputStreamReader(resource.getInputStream(), StandardCharsets.US_ASCII))) {
            return reader.lines().filter(line -> !line.isBlank()).toList();
        } catch (IOException e) {
            throw new UncheckedIOException("Could not read CardDemo data file " + resource, e);
        }
    }

    /** CVACT01Y ACCOUNT-RECORD. */
    static Account parseAccount(String line) {
        var r = new CopybookReader(line);
        var a = new Account();
        a.setId(r.unsignedLong(11));                     // ACCT-ID                PIC 9(11)
        a.setActiveStatus(r.text(1));                    // ACCT-ACTIVE-STATUS     PIC X(01)
        a.setCurrentBalance(r.signedDecimal(10, 2));     // ACCT-CURR-BAL          PIC S9(10)V99
        a.setCreditLimit(r.signedDecimal(10, 2));        // ACCT-CREDIT-LIMIT      PIC S9(10)V99
        a.setCashCreditLimit(r.signedDecimal(10, 2));    // ACCT-CASH-CREDIT-LIMIT PIC S9(10)V99
        a.setOpenDate(r.date());                         // ACCT-OPEN-DATE         PIC X(10)
        a.setExpirationDate(r.date());                   // ACCT-EXPIRAION-DATE    PIC X(10)
        a.setReissueDate(r.date());                      // ACCT-REISSUE-DATE      PIC X(10)
        a.setCurrentCycleCredit(r.signedDecimal(10, 2)); // ACCT-CURR-CYC-CREDIT   PIC S9(10)V99
        a.setCurrentCycleDebit(r.signedDecimal(10, 2));  // ACCT-CURR-CYC-DEBIT    PIC S9(10)V99
        a.setAddressZip(r.text(10));                     // ACCT-ADDR-ZIP          PIC X(10)
        a.setGroupId(r.text(10));                        // ACCT-GROUP-ID          PIC X(10)
        return a;                                        // FILLER                 PIC X(178)
    }

    /** CVCUS01Y CUSTOMER-RECORD. */
    static Customer parseCustomer(String line) {
        var r = new CopybookReader(line);
        var c = new Customer();
        c.setId(r.unsignedLong(9));                      // CUST-ID                  PIC 9(09)
        c.setFirstName(r.text(25));                      // CUST-FIRST-NAME          PIC X(25)
        c.setMiddleName(r.text(25));                     // CUST-MIDDLE-NAME         PIC X(25)
        c.setLastName(r.text(25));                       // CUST-LAST-NAME           PIC X(25)
        c.setAddressLine1(r.text(50));                   // CUST-ADDR-LINE-1         PIC X(50)
        c.setAddressLine2(r.text(50));                   // CUST-ADDR-LINE-2         PIC X(50)
        c.setAddressLine3(r.text(50));                   // CUST-ADDR-LINE-3         PIC X(50)
        c.setStateCode(r.text(2));                       // CUST-ADDR-STATE-CD       PIC X(02)
        c.setCountryCode(r.text(3));                     // CUST-ADDR-COUNTRY-CD     PIC X(03)
        c.setZip(r.text(10));                            // CUST-ADDR-ZIP            PIC X(10)
        c.setPhone1(r.text(15));                         // CUST-PHONE-NUM-1         PIC X(15)
        c.setPhone2(r.text(15));                         // CUST-PHONE-NUM-2         PIC X(15)
        c.setSsn(r.digits(9));                           // CUST-SSN                 PIC 9(09)
        c.setGovernmentId(r.text(20));                   // CUST-GOVT-ISSUED-ID      PIC X(20)
        c.setDateOfBirth(r.date());                      // CUST-DOB-YYYY-MM-DD      PIC X(10)
        c.setEftAccountId(r.text(10));                   // CUST-EFT-ACCOUNT-ID      PIC X(10)
        c.setPrimaryCardHolder(r.text(1));               // CUST-PRI-CARD-HOLDER-IND PIC X(01)
        c.setFicoScore(r.unsignedInt(3));                // CUST-FICO-CREDIT-SCORE   PIC 9(03)
        return c;                                        // FILLER                   PIC X(168)
    }

    /** CVACT02Y CARD-RECORD. */
    static Card parseCard(String line) {
        var r = new CopybookReader(line);
        var c = new Card();
        c.setCardNumber(r.text(16));                     // CARD-NUM            PIC X(16)
        c.setAccountId(r.unsignedLong(11));              // CARD-ACCT-ID        PIC 9(11)
        c.setCvv(r.unsignedInt(3));                      // CARD-CVV-CD         PIC 9(03)
        c.setEmbossedName(r.text(50));                   // CARD-EMBOSSED-NAME  PIC X(50)
        c.setExpirationDate(r.date());                   // CARD-EXPIRAION-DATE PIC X(10)
        c.setActiveStatus(r.text(1));                    // CARD-ACTIVE-STATUS  PIC X(01)
        return c;                                        // FILLER              PIC X(59)
    }

    /** CVACT03Y CARD-XREF-RECORD. */
    static CardXref parseCardXref(String line) {
        var r = new CopybookReader(line);
        var x = new CardXref();
        x.setCardNumber(r.text(16));                     // XREF-CARD-NUM PIC X(16)
        x.setCustomerId(r.unsignedLong(9));              // XREF-CUST-ID  PIC 9(09)
        x.setAccountId(r.unsignedLong(11));              // XREF-ACCT-ID  PIC 9(11)
        return x;                                        // FILLER        PIC X(14)
    }

    /** CVTRA03Y TRAN-TYPE-RECORD. */
    static TransactionType parseTransactionType(String line) {
        var r = new CopybookReader(line);
        var t = new TransactionType();
        t.setCode(r.text(2));                            // TRAN-TYPE      PIC X(02)
        t.setDescription(r.text(50));                    // TRAN-TYPE-DESC PIC X(50)
        return t;                                        // FILLER         PIC X(08)
    }

    /** CVTRA04Y TRAN-CAT-RECORD. */
    static TransactionCategory parseTransactionCategory(String line) {
        var r = new CopybookReader(line);
        var c = new TransactionCategory();
        c.setId(new TransactionCategoryId(
                r.text(2),                               // TRAN-TYPE-CD       PIC X(02)
                r.unsignedInt(4)));                      // TRAN-CAT-CD        PIC 9(04)
        c.setDescription(r.text(50));                    // TRAN-CAT-TYPE-DESC PIC X(50)
        return c;                                        // FILLER             PIC X(04)
    }

    /** CVTRA02Y DIS-GROUP-RECORD. */
    static DisclosureGroup parseDisclosureGroup(String line) {
        var r = new CopybookReader(line);
        var g = new DisclosureGroup();
        g.setId(new DisclosureGroupId(
                r.text(10),                              // DIS-ACCT-GROUP-ID PIC X(10)
                r.text(2),                               // DIS-TRAN-TYPE-CD  PIC X(02)
                r.unsignedInt(4)));                      // DIS-TRAN-CAT-CD   PIC 9(04)
        g.setInterestRate(r.signedDecimal(4, 2));        // DIS-INT-RATE      PIC S9(04)V99
        return g;                                        // FILLER            PIC X(28)
    }

    /** CVTRA01Y TRAN-CAT-BAL-RECORD. */
    static TransactionCategoryBalance parseCategoryBalance(String line) {
        var r = new CopybookReader(line);
        var b = new TransactionCategoryBalance();
        b.setId(new TransactionCategoryBalanceId(
                r.unsignedLong(11),                      // TRANCAT-ACCT-ID PIC 9(11)
                r.text(2),                               // TRANCAT-TYPE-CD PIC X(02)
                r.unsignedInt(4)));                      // TRANCAT-CD      PIC 9(04)
        b.setBalance(r.signedDecimal(9, 2));             // TRAN-CAT-BAL    PIC S9(09)V99
        return b;                                        // FILLER          PIC X(22)
    }
}
