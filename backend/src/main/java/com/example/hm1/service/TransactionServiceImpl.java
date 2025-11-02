package com.example.hm1.service;

import com.example.hm1.dao.TransactionRepository;
import com.example.hm1.entity.Account;
import com.example.hm1.entity.Customer;
import com.example.hm1.entity.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class TransactionServiceImpl implements TransactionService {

    private final TransactionRepository transactionRepository;

    public TransactionServiceImpl(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    @Override
    public Transaction createTransaction(Transaction.TransactionType type, 
                                       BigDecimal amount, 
                                       String description, 
                                       Account fromAccount, 
                                       Account toAccount, 
                                       Customer customer) {
        System.out.println("TransactionServiceImpl.createTransaction: type=" + type + ", amount=" + amount + ", customer=" + (customer != null ? customer.getId() : "null"));
        Transaction transaction = new Transaction(type, amount, description, fromAccount, toAccount, customer);
        
        Transaction saved = transactionRepository.save(transaction);
        System.out.println("TransactionServiceImpl.createTransaction: Saved transaction ID=" + saved.getId());
        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Transaction> getTransactionsByCustomerId(Long customerId) {
        System.out.println("TransactionServiceImpl.getTransactionsByCustomerId: customerId=" + customerId);
        List<Transaction> transactions = transactionRepository.findByCustomerIdOrderByTimestampDesc(customerId);
        System.out.println("TransactionServiceImpl.getTransactionsByCustomerId: Found " + transactions.size() + " transactions");
        if (!transactions.isEmpty()) {
            System.out.println("TransactionServiceImpl.getTransactionsByCustomerId: First transaction: " + transactions.get(0));
        }
        return transactions;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Transaction> getTransactionsByCustomerId(Long customerId, Pageable pageable) {
        return transactionRepository.findByCustomerIdOrderByTimestampDescPage(customerId, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Transaction> getAllTransactions() {
        return transactionRepository.findAllOrderByTimestampDesc();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Transaction> getAllTransactions(Pageable pageable) {
        return transactionRepository.findAllOrderByTimestampDesc(pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Transaction> getTransactionsByDateRange(Long customerId, LocalDateTime startDate, LocalDateTime endDate) {
        return transactionRepository.findByCustomerIdAndDateRange(customerId, startDate, endDate);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Transaction> getAllTransactionsByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        return transactionRepository.findAllByDateRange(startDate, endDate);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Transaction> getTransactionsByType(Transaction.TransactionType type) {
        return transactionRepository.findByTypeOrderByTimestampDesc(type);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Transaction> getTransactionsByStatus(Transaction.TransactionStatus status) {
        return transactionRepository.findByStatusOrderByTimestampDesc(status);
    }

    @Override
    @Transactional(readOnly = true)
    public Transaction getTransactionById(Long id) {
        return transactionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Transaction not found with id: " + id));
    }

    @Override
    public void deleteTransaction(Long id) {
        transactionRepository.deleteById(id);
    }
}
