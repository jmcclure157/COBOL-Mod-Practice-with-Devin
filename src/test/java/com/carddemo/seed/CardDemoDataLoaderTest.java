package com.carddemo.seed;

import com.carddemo.repository.AccountRepository;
import com.carddemo.repository.CardRepository;
import com.carddemo.repository.CardXrefRepository;
import com.carddemo.repository.CustomerRepository;
import com.carddemo.repository.DisclosureGroupRepository;
import com.carddemo.repository.TransactionCategoryBalanceRepository;
import com.carddemo.repository.TransactionCategoryRepository;
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
