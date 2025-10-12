package com.example.hm1.service;

import com.example.hm1.entity.Account;

public interface AccountService {
    boolean deposit(String accountNumber, Double amount);
    boolean withdraw(String accountNumber, Double amount);
    boolean transfer(String fromAccountNumber, String toAccountNumber, Double amount);
    Account getAccountByNumber(String accountNumber);
}