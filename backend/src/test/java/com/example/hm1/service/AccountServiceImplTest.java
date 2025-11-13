package com.example.hm1.service;

import com.example.hm1.dao.AccountRepo;
import com.example.hm1.entity.Account;
import com.example.hm1.entity.Currency;
import com.example.hm1.entity.Customer;
import com.example.hm1.entity.Transaction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountServiceImplTest {

    @Mock
    private AccountRepo accountRepo;
    @Mock
    private TransactionService transactionService;
    @Mock
    private NotificationService notificationService;
    @Mock
    private ExchangeRateService exchangeRateService;

    @InjectMocks
    private AccountServiceImpl accountService;

    private Account usdAccount;
    private Account eurAccount;

    @BeforeEach
    void setUp() {
        Customer customer = new Customer();
        customer.setId(1L);
        customer.setName("John Doe");

        usdAccount = Account.builder()
                .id(10L)
                .number("ACC-001")
                .currency(Currency.USD)
                .balance(1000.0)
                .customer(customer)
                .build();

        eurAccount = Account.builder()
                .id(20L)
                .number("ACC-002")
                .currency(Currency.EUR)
                .balance(500.0)
                .customer(customer)
                .build();
    }

    @Test
    void depositReturnsFalseWhenAmountIsNonPositive() {
        boolean result = accountService.deposit("ACC-001", 0.0);

        assertThat(result).isFalse();
        verifyNoInteractions(accountRepo, transactionService, notificationService, exchangeRateService);
    }

    @Test
    void depositUpdatesBalanceAndPersistsTransaction() {
        when(accountRepo.findByNumber("ACC-001")).thenReturn(usdAccount);
        when(accountRepo.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));

        boolean result = accountService.deposit("ACC-001", 200.0);

        assertThat(result).isTrue();

        ArgumentCaptor<Account> accountCaptor = ArgumentCaptor.forClass(Account.class);
        verify(accountRepo, times(1)).save(accountCaptor.capture());
        assertThat(accountCaptor.getValue().getBalance()).isEqualTo(1200.0);

        verify(transactionService).createTransaction(
                eq(Transaction.TransactionType.DEPOSIT),
                eq(BigDecimal.valueOf(200.0)),
                contains("ACC-001"),
                isNull(),
                eq(usdAccount),
                eq(usdAccount.getCustomer())
        );

        verify(notificationService).sendAccountUpdateNotification("ACC-001", "DEPOSIT", 200.0, 1200.0);
    }

    @Test
    void depositReturnsFalseWhenAccountNotFound() {
        when(accountRepo.findByNumber("ACC-404")).thenReturn(null);

        boolean result = accountService.deposit("ACC-404", 50.0);

        assertThat(result).isFalse();
        verify(accountRepo, never()).save(any());
        verifyNoInteractions(transactionService, notificationService, exchangeRateService);
    }

    @Test
    void withdrawReturnsFalseWhenAmountIsNonPositive() {
        boolean result = accountService.withdraw("ACC-001", 0.0);

        assertThat(result).isFalse();
        verifyNoInteractions(accountRepo, transactionService, notificationService, exchangeRateService);
    }

    @Test
    void withdrawReturnsFalseWhenAccountMissingOrInsufficientFunds() {
        when(accountRepo.findByNumber("ACC-001")).thenReturn(null, usdAccount);

        assertThat(accountService.withdraw("ACC-001", 50.0)).isFalse();

        usdAccount.setBalance(20.0);
        assertThat(accountService.withdraw("ACC-001", 50.0)).isFalse();
        verify(accountRepo, never()).save(any());
        verifyNoInteractions(transactionService, notificationService);
    }

    @Test
    void withdrawUpdatesBalanceAndRecordsTransaction() {
        when(accountRepo.findByNumber("ACC-001")).thenReturn(usdAccount);
        when(accountRepo.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));

        boolean result = accountService.withdraw("ACC-001", 100.0);

        assertThat(result).isTrue();
        assertThat(usdAccount.getBalance()).isEqualTo(900.0);

        verify(accountRepo).save(usdAccount);
        verify(transactionService).createTransaction(
                eq(Transaction.TransactionType.WITHDRAWAL),
                eq(BigDecimal.valueOf(100.0)),
                contains("ACC-001"),
                eq(usdAccount),
                isNull(),
                eq(usdAccount.getCustomer())
        );
        verify(notificationService).sendAccountUpdateNotification("ACC-001", "WITHDRAWAL", 100.0, 900.0);
    }

    @Test
    void transferReturnsFalseForInvalidAmountsOrAccounts() {
        assertThat(accountService.transfer("ACC-001", "ACC-002", -10.0)).isFalse();
        verifyNoInteractions(accountRepo, transactionService, notificationService, exchangeRateService);

        when(accountRepo.findByNumber("ACC-001")).thenReturn(null);
        assertThat(accountService.transfer("ACC-001", "ACC-002", 100.0)).isFalse();

        when(accountRepo.findByNumber("ACC-001")).thenReturn(usdAccount);
        when(accountRepo.findByNumber("ACC-002")).thenReturn(null);
        assertThat(accountService.transfer("ACC-001", "ACC-002", 100.0)).isFalse();

        when(accountRepo.findByNumber("ACC-002")).thenReturn(eurAccount);
        usdAccount.setBalance(50.0);
        assertThat(accountService.transfer("ACC-001", "ACC-002", 100.0)).isFalse();
        verify(transactionService, never()).createTransaction(any(), any(), any(), any(), any(), any());
    }

    @Test
    void transferSameCurrencyMovesFundsWithoutConversion() {
        eurAccount.setCurrency(Currency.USD);
        when(accountRepo.findByNumber("ACC-001")).thenReturn(usdAccount);
        when(accountRepo.findByNumber("ACC-002")).thenReturn(eurAccount);
        when(accountRepo.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));

        boolean result = accountService.transfer("ACC-001", "ACC-002", 150.0);

        assertThat(result).isTrue();
        assertThat(usdAccount.getBalance()).isEqualTo(850.0);
        assertThat(eurAccount.getBalance()).isEqualTo(650.0);

        verify(transactionService).createTransaction(
                eq(Transaction.TransactionType.TRANSFER),
                eq(BigDecimal.valueOf(150.0)),
                contains("ACC-001"),
                eq(usdAccount),
                eq(eurAccount),
                eq(usdAccount.getCustomer())
        );

        verify(notificationService).sendAccountUpdateNotification("ACC-001", "TRANSFER_OUT", 150.0, 850.0);
        verify(notificationService).sendAccountUpdateNotification("ACC-002", "TRANSFER_IN", 150.0, 650.0);
        verify(exchangeRateService, never()).convertAmount(any(), any(), any());
    }

    @Test
    void transferDifferentCurrenciesUsesConversion() {
        usdAccount.setBalance(1000.0);
        eurAccount.setBalance(500.0);

        when(accountRepo.findByNumber("ACC-001")).thenReturn(usdAccount);
        when(accountRepo.findByNumber("ACC-002")).thenReturn(eurAccount);
        when(accountRepo.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(exchangeRateService.convertAmount(BigDecimal.valueOf(200.0), Currency.USD, Currency.EUR))
                .thenReturn(new BigDecimal("180.00"));

        boolean result = accountService.transfer("ACC-001", "ACC-002", 200.0);

        assertThat(result).isTrue();
        assertThat(usdAccount.getBalance()).isEqualTo(800.0);
        assertThat(eurAccount.getBalance()).isEqualTo(680.0);

        verify(exchangeRateService).convertAmount(BigDecimal.valueOf(200.0), Currency.USD, Currency.EUR);
        verify(notificationService).sendAccountUpdateNotification("ACC-001", "TRANSFER_OUT", 200.0, 800.0);
        verify(notificationService).sendAccountUpdateNotification("ACC-002", "TRANSFER_IN", 180.0, 680.0);
    }

    @Test
    void getAccountByNumberDelegatesToRepository() {
        when(accountRepo.findByNumber("ACC-001")).thenReturn(usdAccount);

        Account result = accountService.getAccountByNumber("ACC-001");

        assertThat(result).isSameAs(usdAccount);
        verify(accountRepo).findByNumber("ACC-001");
    }
}

