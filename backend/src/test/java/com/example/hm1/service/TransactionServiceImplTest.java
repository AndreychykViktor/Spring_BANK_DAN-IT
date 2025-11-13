package com.example.hm1.service;

import com.example.hm1.dao.TransactionRepository;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionServiceImplTest {

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private TransactionServiceImpl transactionService;

    private Customer customer;
    private Account fromAccount;
    private Account toAccount;

    @BeforeEach
    void setUp() {
        customer = Customer.builder()
                .id(1L)
                .name("Jane Customer")
                .email("jane@example.com")
                .age(30)
                .build();

        fromAccount = Account.builder()
                .id(100L)
                .number("ACC-100")
                .currency(Currency.USD)
                .balance(1000.0)
                .customer(customer)
                .build();

        toAccount = Account.builder()
                .id(200L)
                .number("ACC-200")
                .currency(Currency.EUR)
                .balance(500.0)
                .customer(customer)
                .build();
    }

    @Test
    void createTransactionPersistsEntityWithProvidedData() {
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> {
            Transaction tx = invocation.getArgument(0);
            tx.setId(42L);
            return tx;
        });

        Transaction result = transactionService.createTransaction(
                Transaction.TransactionType.TRANSFER,
                BigDecimal.valueOf(250),
                "Test transfer",
                fromAccount,
                toAccount,
                customer
        );

        ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository).save(captor.capture());

        Transaction saved = captor.getValue();
        assertAll(
                () -> assertThat(saved.getType()).isEqualTo(Transaction.TransactionType.TRANSFER),
                () -> assertThat(saved.getAmount()).isEqualByComparingTo("250"),
                () -> assertThat(saved.getDescription()).isEqualTo("Test transfer"),
                () -> assertThat(saved.getFromAccount()).isSameAs(fromAccount),
                () -> assertThat(saved.getToAccount()).isSameAs(toAccount),
                () -> assertThat(saved.getCustomer()).isSameAs(customer),
                () -> assertThat(saved.getStatus()).isEqualTo(Transaction.TransactionStatus.COMPLETED),
                () -> assertThat(result.getId()).isEqualTo(42L)
        );
    }

    @Test
    void getTransactionsByCustomerIdReturnsOrderedList() {
        List<Transaction> transactions = List.of(
                new Transaction(Transaction.TransactionType.DEPOSIT, BigDecimal.TEN, "Deposit", null, toAccount, customer),
                new Transaction(Transaction.TransactionType.WITHDRAWAL, BigDecimal.ONE, "Withdrawal", fromAccount, null, customer)
        );
        when(transactionRepository.findByCustomerIdOrderByTimestampDesc(1L)).thenReturn(transactions);

        List<Transaction> result = transactionService.getTransactionsByCustomerId(1L);

        assertThat(result).isEqualTo(transactions);
        verify(transactionRepository).findByCustomerIdOrderByTimestampDesc(1L);
    }

    @Test
    void getTransactionsByCustomerIdWithPagingDelegatesToRepository() {
        PageRequest pageRequest = PageRequest.of(0, 10);
        Page<Transaction> page = new PageImpl<>(List.of());
        when(transactionRepository.findByCustomerIdOrderByTimestampDescPage(1L, pageRequest)).thenReturn(page);

        Page<Transaction> result = transactionService.getTransactionsByCustomerId(1L, pageRequest);

        assertThat(result).isSameAs(page);
        verify(transactionRepository).findByCustomerIdOrderByTimestampDescPage(1L, pageRequest);
    }

    @Test
    void getAllTransactionsSupportsListAndPagedVariants() {
        List<Transaction> allTransactions = List.of(
                new Transaction(Transaction.TransactionType.DEPOSIT, BigDecimal.ONE, "Deposit", null, toAccount, customer)
        );
        when(transactionRepository.findAllOrderByTimestampDesc()).thenReturn(allTransactions);

        List<Transaction> listResult = transactionService.getAllTransactions();
        assertThat(listResult).isEqualTo(allTransactions);
        verify(transactionRepository).findAllOrderByTimestampDesc();

        PageRequest pageRequest = PageRequest.of(0, 20);
        Page<Transaction> page = new PageImpl<>(allTransactions);
        when(transactionRepository.findAllOrderByTimestampDesc(pageRequest)).thenReturn(page);

        Page<Transaction> pageResult = transactionService.getAllTransactions(pageRequest);
        assertThat(pageResult).isSameAs(page);
        verify(transactionRepository).findAllOrderByTimestampDesc(pageRequest);
    }

    @Test
    void getTransactionsByDateRangeDelegatesToRepository() {
        LocalDateTime start = LocalDateTime.now().minusDays(7);
        LocalDateTime end = LocalDateTime.now();
        List<Transaction> expected = List.of(new Transaction(Transaction.TransactionType.DEPOSIT, BigDecimal.ONE, "Deposit", null, toAccount, customer));
        when(transactionRepository.findByCustomerIdAndDateRange(1L, start, end)).thenReturn(expected);

        List<Transaction> result = transactionService.getTransactionsByDateRange(1L, start, end);

        assertThat(result).isEqualTo(expected);
        verify(transactionRepository).findByCustomerIdAndDateRange(1L, start, end);
    }

    @Test
    void getAllTransactionsByDateRangeDelegatesToRepository() {
        LocalDateTime start = LocalDateTime.now().minusDays(1);
        LocalDateTime end = LocalDateTime.now();
        List<Transaction> expected = List.of(new Transaction(Transaction.TransactionType.TRANSFER, BigDecimal.TEN, "Transfer", fromAccount, toAccount, customer));
        when(transactionRepository.findAllByDateRange(start, end)).thenReturn(expected);

        List<Transaction> result = transactionService.getAllTransactionsByDateRange(start, end);

        assertThat(result).isEqualTo(expected);
        verify(transactionRepository).findAllByDateRange(start, end);
    }

    @Test
    void getTransactionsByTypeAndStatusDelegateToRepository() {
        List<Transaction> byType = List.of(new Transaction(Transaction.TransactionType.DEPOSIT, BigDecimal.ONE, "Deposit", null, toAccount, customer));
        when(transactionRepository.findByTypeOrderByTimestampDesc(Transaction.TransactionType.DEPOSIT)).thenReturn(byType);
        assertThat(transactionService.getTransactionsByType(Transaction.TransactionType.DEPOSIT)).isEqualTo(byType);
        verify(transactionRepository).findByTypeOrderByTimestampDesc(Transaction.TransactionType.DEPOSIT);

        List<Transaction> byStatus = List.of(new Transaction(Transaction.TransactionType.TRANSFER, BigDecimal.TEN, "Transfer", fromAccount, toAccount, customer));
        when(transactionRepository.findByStatusOrderByTimestampDesc(Transaction.TransactionStatus.COMPLETED)).thenReturn(byStatus);
        assertThat(transactionService.getTransactionsByStatus(Transaction.TransactionStatus.COMPLETED)).isEqualTo(byStatus);
        verify(transactionRepository).findByStatusOrderByTimestampDesc(Transaction.TransactionStatus.COMPLETED);
    }

    @Test
    void getTransactionByIdReturnsEntityWhenPresent() {
        Transaction transaction = new Transaction(Transaction.TransactionType.PAYMENT, BigDecimal.TEN, "Payment", fromAccount, toAccount, customer);
        when(transactionRepository.findById(99L)).thenReturn(Optional.of(transaction));

        Transaction result = transactionService.getTransactionById(99L);

        assertThat(result).isSameAs(transaction);
        verify(transactionRepository).findById(99L);
    }

    @Test
    void getTransactionByIdThrowsWhenMissing() {
        when(transactionRepository.findById(100L)).thenReturn(Optional.empty());

        Throwable thrown = org.assertj.core.api.Assertions.catchThrowable(() -> transactionService.getTransactionById(100L));
        assertThat(thrown)
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("100");
    }

    @Test
    void deleteTransactionDelegatesToRepository() {
        transactionService.deleteTransaction(55L);
        verify(transactionRepository).deleteById(55L);
    }
}

