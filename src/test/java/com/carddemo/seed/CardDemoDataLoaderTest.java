package com.carddemo.seed;

import com.carddemo.repository.AccountRepository;
import com.carddemo.repository.CardRepository;
import com.carddemo.repository.CardXrefRepository;
import com.carddemo.repository.CustomerRepository;
import com.carddemo.repository.DisclosureGroupRepository;
import com.carddemo.repository.TransactionCategoryBalanceRepository;
import com.carddemo.repository.TransactionCategoryRepository;
import com.carddemo.repository.TransactionRepository;
import com.carddemo.repository.TransactionTypeRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class CardDemoDataLoaderTest {

    @Autowired AccountRepository accounts;
    @Autowired CustomerRepository customers;
    @Autowired CardRepository cards;
    @Autowired CardXrefRepository cardXrefs;
    @Autowired TransactionTypeRepository transactionTypes;
    @Autowired TransactionCategoryRepository transactionCategories;
    @Autowired DisclosureGroupRepository disclosureGroups;
    @Autowired TransactionCategoryBalanceRepository categoryBalances;
    @Autowired TransactionRepository transactions;

    @Test
    void seedsEveryRecordFromTheSampleFiles() {
        assertThat(accounts.count()).isEqualTo(50);
        assertThat(customers.count()).isEqualTo(50);
        assertThat(cards.count()).isEqualTo(50);
        assertThat(cardXrefs.count()).isEqualTo(50);
        assertThat(transactionTypes.count()).isEqualTo(7);
        assertThat(transactionCategories.count()).isEqualTo(18);
        assertThat(disclosureGroups.count()).isEqualTo(51);
        assertThat(categoryBalances.count()).isEqualTo(50);
        assertThat(transactions.count()).isEqualTo(300);
    }

    @Test
    void parsesDailyTransactionsIncludingNegativeAmounts() {
        var purchase = transactions.findById("0000000000683580").orElseThrow();
        assertThat(purchase.getDescription()).isEqualTo("Purchase at Abshire-Lowe");
        assertThat(purchase.getAmount()).isEqualByComparingTo(new BigDecimal("504.77"));   // 0000005047G
        assertThat(purchase.getCardNumber()).isEqualTo("4859452612877065");
        assertThat(purchase.getOriginTimestamp()).isEqualTo("2022-06-10 19:27:53.000000");

        var refund = transactions.findById("0000000001774260").orElseThrow();
        assertThat(refund.getAmount()).isEqualByComparingTo(new BigDecimal("-919.00"));    // 0000009190}

        var otherRefund = transactions.findById("0000000016259484").orElseThrow();
        assertThat(otherRefund.getAmount()).isEqualByComparingTo(new BigDecimal("-56.77")); // 0000000567P
    }

    @Test
    void parsesAccountOneLikeTheCopybookSays() {
        var account = accounts.findById(1L).orElseThrow();
        assertThat(account.getActiveStatus()).isEqualTo("Y");
        assertThat(account.getCurrentBalance()).isEqualByComparingTo(new BigDecimal("194.00"));
        assertThat(account.getCreditLimit()).isEqualByComparingTo(new BigDecimal("2020.00"));
        assertThat(account.getCashCreditLimit()).isEqualByComparingTo(new BigDecimal("1020.00"));
        assertThat(account.getOpenDate()).isEqualTo(LocalDate.of(2014, 11, 20));
    }

    @Test
    void parsesCustomerOne() {
        var customer = customers.findById(1L).orElseThrow();
        assertThat(customer.getFirstName()).isEqualTo("Immanuel");
        assertThat(customer.getLastName()).isEqualTo("Kessler");
        assertThat(customer.getSsn()).isEqualTo("020973888");
        assertThat(customer.getFicoScore()).isEqualTo(274);
        assertThat(customer.getDateOfBirth()).isEqualTo(LocalDate.of(1961, 6, 8));
    }
}
