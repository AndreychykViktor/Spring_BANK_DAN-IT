package com.example.hm1.service;

import com.example.hm1.dao.AccountRepo;
import com.example.hm1.entity.Account;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AccountServiceImpl implements AccountService {

    private final AccountRepo accountRepo;

    @Autowired
    public AccountServiceImpl(AccountRepo accountRepo) {
        this.accountRepo = accountRepo;
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
        return true;
    }

    @Override
    public Account getAccountByNumber(String accountNumber) {
        return accountRepo.findByNumber(accountNumber);
    }
}