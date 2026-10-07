package com.carddemo.service;

import com.carddemo.model.Account;
import com.carddemo.model.CardXref;
import com.carddemo.model.Customer;
import com.carddemo.repository.AccountRepository;
import com.carddemo.repository.CardXrefRepository;
import com.carddemo.repository.CustomerRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountViewServiceTest {

    @Mock CardXrefRepository cardXrefs;
    @Mock AccountRepository accounts;
    @Mock CustomerRepository customers;
    @InjectMocks AccountViewService service;

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "*"})
    void blankOrStarMeansNotProvided(String input) {
        assertThatThrownBy(() -> service.viewAccount(input))
                .isInstanceOf(InvalidAccountIdException.class)
                .hasMessage("Account number not provided");
        verifyNoInteractions(cardXrefs, accounts, customers);
    }

    @ParameterizedTest
    @ValueSource(strings = {"abc", "12a", "-1", "0", "00000000000", "123456789012"})
    void rejectsNonNumericZeroOrTooLong(String input) {
        assertThatThrownBy(() -> service.viewAccount(input))
                .isInstanceOf(InvalidAccountIdException.class)
                .hasMessage("Account Filter must  be a non-zero 11 digit number");
        verifyNoInteractions(cardXrefs, accounts, customers);
    }

    @Test
    void missingCrossReferenceIsNotFound() {
        when(cardXrefs.findFirstByAccountIdOrderByCardNumberAsc(42L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.viewAccount("42"))
                .isInstanceOf(AccountNotFoundException.class)
                .hasMessage("Account:00000000042 not found in Cross ref file.");
    }

    @Test
    void missingAccountIsNotFound() {
        when(cardXrefs.findFirstByAccountIdOrderByCardNumberAsc(42L)).thenReturn(Optional.of(xref(42L, 7L)));
        when(accounts.findById(42L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.viewAccount("42"))
                .isInstanceOf(AccountNotFoundException.class)
                .hasMessage("Account:00000000042 not found in Acct Master file.");
    }

    @Test
    void missingCustomerIsNotFound() {
        when(cardXrefs.findFirstByAccountIdOrderByCardNumberAsc(42L)).thenReturn(Optional.of(xref(42L, 7L)));
        when(accounts.findById(42L)).thenReturn(Optional.of(account(42L)));
        when(customers.findById(7L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.viewAccount("42"))
                .isInstanceOf(AccountNotFoundException.class)
                .hasMessage("CustId:000000007 not found in customer master.");
    }

    @Test
    void combinesAccountAndCustomerAndFormatsSsn() {
        when(cardXrefs.findFirstByAccountIdOrderByCardNumberAsc(42L)).thenReturn(Optional.of(xref(42L, 7L)));
        when(accounts.findById(42L)).thenReturn(Optional.of(account(42L)));
        var customer = new Customer();
        customer.setId(7L);
        customer.setSsn("020973888");
        customer.setAddressLine3("Springfield");
        when(customers.findById(7L)).thenReturn(Optional.of(customer));

        AccountView view = service.viewAccount("00000000042");

        assertThat(view.accountId()).isEqualTo(42L);
        assertThat(view.customer().customerId()).isEqualTo(7L);
        assertThat(view.customer().ssn()).isEqualTo("020-97-3888");
        assertThat(view.customer().city()).isEqualTo("Springfield");
    }

    private static CardXref xref(long accountId, long customerId) {
        var x = new CardXref();
        x.setCardNumber("0000000000000042");
        x.setAccountId(accountId);
        x.setCustomerId(customerId);
        return x;
    }

    private static Account account(long id) {
        var a = new Account();
        a.setId(id);
        return a;
    }
}
