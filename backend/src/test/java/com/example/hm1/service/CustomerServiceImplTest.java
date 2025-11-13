package com.example.hm1.service;

import com.example.hm1.dao.AccountRepo;
import com.example.hm1.dao.CustomerRepo;
import com.example.hm1.entity.Account;
import com.example.hm1.entity.Currency;
import com.example.hm1.entity.Customer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomerServiceImplTest {

    @Mock
    private CustomerRepo customerRepo;

    @Mock
    private AccountRepo accountRepo;

    @InjectMocks
    private CustomerServiceImpl customerService;

    private Customer customer;

    @BeforeEach
    void setUp() {
        customer = Customer.builder()
                .id(1L)
                .name("Alice")
                .email("alice@example.com")
                .age(28)
                .build();
    }

    @Test
    void createCustomerDelegatesToRepository() {
        when(customerRepo.save(any(Customer.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Customer created = customerService.createCustomer("Bob", 30);

        ArgumentCaptor<Customer> captor = ArgumentCaptor.forClass(Customer.class);
        verify(customerRepo).save(captor.capture());

        Customer saved = captor.getValue();
        assertThat(saved.getName()).isEqualTo("Bob");
        assertThat(saved.getAge()).isEqualTo(30);
        assertThat(saved.getEmail()).isEqualTo("temp@email.com");
        assertThat(created).isSameAs(saved);
    }

    @Test
    void updateCustomerReturnsSavedEntity() {
        when(customerRepo.save(customer)).thenReturn(customer);

        Customer updated = customerService.updateCustomer(customer);

        assertThat(updated).isSameAs(customer);
        verify(customerRepo).save(customer);
    }

    @Test
    void getCustomerByIdLoadsAccounts() {
        List<Account> accounts = List.of(Account.builder().id(10L).number("ACC-001").currency(Currency.UAH).balance(100.0).customer(customer).build());
        when(customerRepo.getOne(1L)).thenReturn(customer);
        when(accountRepo.findByCustomerId(1L)).thenReturn(accounts);

        Customer result = customerService.getCustomerById(1L);

        assertThat(result.getAccounts()).isEqualTo(accounts);
        verify(accountRepo).findByCustomerId(1L);
    }

    @Test
    void getAllCustomersPopulatesAccountsForEach() {
        Customer another = Customer.builder().id(2L).name("Eve").email("eve@example.com").age(35).build();
        when(customerRepo.findAll()).thenReturn(List.of(customer, another));
        when(accountRepo.findByCustomerId(1L)).thenReturn(List.of());
        when(accountRepo.findByCustomerId(2L)).thenReturn(List.of());

        List<Customer> result = customerService.getAllCustomers();

        assertThat(result).hasSize(2);
        verify(accountRepo).findByCustomerId(1L);
        verify(accountRepo).findByCustomerId(2L);
    }

    @Test
    void deleteCustomerRemovesAccountsBeforeDeletingCustomer() {
        Account account1 = Account.builder().id(10L).number("ACC-001").currency(Currency.USD).balance(100.0).customer(customer).build();
        Account account2 = Account.builder().id(11L).number("ACC-002").currency(Currency.EUR).balance(50.0).customer(customer).build();
        when(accountRepo.findByCustomerId(1L)).thenReturn(List.of(account1, account2));
        when(customerRepo.deleteById(1L)).thenReturn(true);

        boolean result = customerService.deleteCustomer(1L);

        assertThat(result).isTrue();
        verify(accountRepo).delete(account1);
        verify(accountRepo).delete(account2);
        verify(customerRepo).deleteById(1L);
    }

    @Test
    void createAccountForCustomerThrowsWhenCustomerMissing() {
        when(customerRepo.getOne(1L)).thenReturn(null);

        assertThatThrownBy(() -> customerService.createAccountForCustomer(1L, Currency.USD, "alice@example.com", "secret"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Customer not found");
    }

    @Test
    void createAccountForCustomerGeneratesSequentialNumber() {
        when(customerRepo.getOne(1L)).thenReturn(customer);
        when(accountRepo.getLastAccountNumber()).thenReturn("ACC-0009");
        when(accountRepo.save(any(Account.class))).thenAnswer(invocation -> {
            Account saved = invocation.getArgument(0);
            saved.setId(42L);
            return saved;
        });

        Account result = customerService.createAccountForCustomer(1L, Currency.GBP, "alice@example.com", "secret");

        assertThat(result.getNumber()).isEqualTo("ACC-0010");
        assertThat(result.getCurrency()).isEqualTo(Currency.GBP);
        assertThat(result.getCustomer()).isSameAs(customer);
        verify(accountRepo).save(result);
    }

    @Test
    void deleteAccountFromCustomerValidatesOwnership() {
        Account ownedAccount = Account.builder().id(100L).number("ACC-100").currency(Currency.UAH).balance(0.0).customer(customer).build();
        Customer otherCustomer = Customer.builder().id(2L).name("Other").email("other@example.com").age(40).build();
        Account foreignAccount = Account.builder().id(200L).number("ACC-200").currency(Currency.UAH).balance(0.0).customer(otherCustomer).build();

        when(customerRepo.getOne(1L)).thenReturn(customer);
        when(accountRepo.getOne(100L)).thenReturn(ownedAccount);
        when(accountRepo.getOne(200L)).thenReturn(foreignAccount);
        when(accountRepo.delete(ownedAccount)).thenReturn(true);

        assertThat(customerService.deleteAccountFromCustomer(1L, 100L)).isTrue();
        assertThat(customerService.deleteAccountFromCustomer(1L, 200L)).isFalse();

        when(customerRepo.getOne(3L)).thenReturn(null);
        assertThat(customerService.deleteAccountFromCustomer(3L, 100L)).isFalse();
    }
}

