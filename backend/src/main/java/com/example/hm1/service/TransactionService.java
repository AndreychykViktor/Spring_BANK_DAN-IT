package com.example.hm1.service;

import com.example.hm1.entity.Account;
import com.example.hm1.entity.Customer;
import com.example.hm1.entity.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface TransactionService {
    
    Transaction createTransaction(Transaction.TransactionType type, 
                                BigDecimal amount, 
                                String description, 
                                Account fromAccount, 
                                Account toAccount, 
                                Customer customer);
    
    List<Transaction> getTransactionsByCustomerId(Long customerId);
    
    Page<Transaction> getTransactionsByCustomerId(Long customerId, Pageable pageable);
    
    List<Transaction> getAllTransactions();
    
    Page<Transaction> getAllTransactions(Pageable pageable);
    
    List<Transaction> getTransactionsByDateRange(Long customerId, LocalDateTime startDate, LocalDateTime endDate);
    
    List<Transaction> getAllTransactionsByDateRange(LocalDateTime startDate, LocalDateTime endDate);
    
    List<Transaction> getTransactionsByType(Transaction.TransactionType type);
    
    List<Transaction> getTransactionsByStatus(Transaction.TransactionStatus status);
    
    Transaction getTransactionById(Long id);
    
    void deleteTransaction(Long id);
}
