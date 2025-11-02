package com.example.hm1.service;

import com.example.hm1.dao.AccountRepo;
import com.example.hm1.entity.Account;
import com.example.hm1.entity.Currency;
import com.example.hm1.entity.Customer;
import com.example.hm1.entity.Transaction;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@Transactional
public class AccountServiceImpl implements AccountService {

    private final AccountRepo accountRepo;
    private final TransactionService transactionService;
    private final NotificationService notificationService;
    private final ExchangeRateService exchangeRateService;

    public AccountServiceImpl(AccountRepo accountRepo, TransactionService transactionService, NotificationService notificationService, ExchangeRateService exchangeRateService) {
        this.accountRepo = accountRepo;
        this.transactionService = transactionService;
        this.notificationService = notificationService;
        this.exchangeRateService = exchangeRateService;
    }

    @Override
    public boolean deposit(String accountNumber, Double amount) {
        if (amount <= 0) {
            return false;
        }

        Account account = accountRepo.findByNumber(accountNumber);
        if (account == null) {
            System.err.println("AccountServiceImpl.deposit: Account not found: " + accountNumber);
            return false;
        }

        account.setBalance(account.getBalance() + amount);
        accountRepo.save(account);
        
        // Записуємо транзакцію
        Customer customer = account.getCustomer();
        System.out.println("AccountServiceImpl.deposit: Account customer=" + (customer != null ? customer.getId() : "NULL"));
        if (customer == null) {
            System.err.println("AccountServiceImpl.deposit: ERROR - account.getCustomer() returned NULL for account: " + accountNumber);
        }
        
        transactionService.createTransaction(
            Transaction.TransactionType.DEPOSIT,
            BigDecimal.valueOf(amount),
            "Deposit to account " + accountNumber,
            null,
            account,
            customer
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

        // Конвертація валют якщо рахунки різні валюти
        Currency fromCurrency = fromAccount.getCurrency();
        Currency toCurrency = toAccount.getCurrency();
        
        BigDecimal originalAmount = BigDecimal.valueOf(amount);
        BigDecimal convertedAmount = originalAmount;
        String description;
        
        if (fromCurrency != toCurrency) {
            // Виконуємо конвертацію
            convertedAmount = exchangeRateService.convertAmount(originalAmount, fromCurrency, toCurrency);
            description = String.format("Transfer from %s to %s (converted %s %.2f -> %s %.2f)",
                fromAccountNumber, toAccountNumber,
                fromCurrency.name(), originalAmount.doubleValue(),
                toCurrency.name(), convertedAmount.doubleValue());
            
            System.out.println("AccountServiceImpl.transfer: Currency conversion - " +
                originalAmount.doubleValue() + " " + fromCurrency.name() + " = " +
                convertedAmount.doubleValue() + " " + toCurrency.name());
        } else {
            description = "Transfer from " + fromAccountNumber + " to " + toAccountNumber;
        }

        // Виконуємо операцію: знімаємо з одного рахунку, додаємо до іншого
        fromAccount.setBalance(fromAccount.getBalance() - amount);
        toAccount.setBalance(toAccount.getBalance() + convertedAmount.doubleValue());

        accountRepo.save(fromAccount);
        accountRepo.save(toAccount);
        
        // Записуємо транзакцію з оригінальною сумою (що було знято)
        transactionService.createTransaction(
            Transaction.TransactionType.TRANSFER,
            originalAmount,
            description,
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
            convertedAmount.doubleValue(), 
            toAccount.getBalance().doubleValue()
        );
        
        return true;
    }

    @Override
    public Account getAccountByNumber(String accountNumber) {
        return accountRepo.findByNumber(accountNumber);
    }
}