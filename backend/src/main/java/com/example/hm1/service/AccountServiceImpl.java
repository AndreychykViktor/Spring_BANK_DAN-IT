package com.example.hm1.service;

import com.example.hm1.dao.AccountRepo;
import com.example.hm1.entity.Account;
import com.example.hm1.entity.Customer;
import com.example.hm1.entity.Transaction;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@Transactional
public class AccountServiceImpl implements AccountService {

    private final AccountRepo accountRepo;
    private final TransactionService transactionService;
    private final NotificationService notificationService;

    public AccountServiceImpl(AccountRepo accountRepo, TransactionService transactionService, NotificationService notificationService) {
        this.accountRepo = accountRepo;
        this.transactionService = transactionService;
        this.notificationService = notificationService;
    }

    @Override
    public boolean deposit(String accountNumber, Double amount) {
        if (amount <= 0) {
            return false;
        }

        Account account = accountRepo.findByNumber(accountNumber);
        if (account == null) {
            return false;
        }

        account.setBalance(account.getBalance() + amount);
        accountRepo.save(account);
        
        // Записуємо транзакцію
        transactionService.createTransaction(
            Transaction.TransactionType.DEPOSIT,
            BigDecimal.valueOf(amount),
            "Deposit to account " + accountNumber,
            null,
            account,
            account.getCustomer()
        );
        
        // Відправляємо повідомлення через WebSocket
        notificationService.sendAccountUpdateNotification(
            accountNumber, 
            "DEPOSIT", 
            amount, 
            account.getBalance().doubleValue()
        );
        
        return true;
    }

    @Override
    public boolean withdraw(String accountNumber, Double amount) {
        if (amount <= 0) {
            return false;
        }

        Account account = accountRepo.findByNumber(accountNumber);
        if (account == null || account.getBalance() < amount) {
            return false;
        }

        account.setBalance(account.getBalance() - amount);
        accountRepo.save(account);
        
        // Записуємо транзакцію
        transactionService.createTransaction(
            Transaction.TransactionType.WITHDRAWAL,
            BigDecimal.valueOf(amount),
            "Withdrawal from account " + accountNumber,
            account,
            null,
            account.getCustomer()
        );
        
        // Відправляємо повідомлення через WebSocket
        notificationService.sendAccountUpdateNotification(
            accountNumber, 
            "WITHDRAWAL", 
            amount, 
            account.getBalance().doubleValue()
        );
        
        return true;
    }

    @Override
    public boolean transfer(String fromAccountNumber, String toAccountNumber, Double amount) {
        if (amount <= 0) {
            return false;
        }

        Account fromAccount = accountRepo.findByNumber(fromAccountNumber);
        Account toAccount = accountRepo.findByNumber(toAccountNumber);

        if (fromAccount == null || toAccount == null || fromAccount.getBalance() < amount) {
            return false;
        }

        fromAccount.setBalance(fromAccount.getBalance() - amount);
        toAccount.setBalance(toAccount.getBalance() + amount);

        accountRepo.save(fromAccount);
        accountRepo.save(toAccount);
        
        // Записуємо транзакцію
        transactionService.createTransaction(
            Transaction.TransactionType.TRANSFER,
            BigDecimal.valueOf(amount),
            "Transfer from " + fromAccountNumber + " to " + toAccountNumber,
            fromAccount,
            toAccount,
            fromAccount.getCustomer()
        );
        
        // Відправляємо повідомлення через WebSocket для обох акаунтів
        notificationService.sendAccountUpdateNotification(
            fromAccountNumber, 
            "TRANSFER_OUT", 
            amount, 
            fromAccount.getBalance().doubleValue()
        );
        
        notificationService.sendAccountUpdateNotification(
            toAccountNumber, 
            "TRANSFER_IN", 
            amount, 
            toAccount.getBalance().doubleValue()
        );
        
        return true;
    }

    @Override
    public Account getAccountByNumber(String accountNumber) {
        return accountRepo.findByNumber(accountNumber);
    }
}