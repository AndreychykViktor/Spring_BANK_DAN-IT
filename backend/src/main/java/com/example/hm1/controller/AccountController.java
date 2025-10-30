
package com.example.hm1.controller;

import com.example.hm1.dao.AccountRepo;
import com.example.hm1.entity.Account;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Comparator;
import java.util.List;

@RestController
@RequestMapping("/api/accounts")
@CrossOrigin(origins = "*")
public class AccountController {

    private final AccountRepo accountRepo;

    public AccountController(AccountRepo accountRepo) {
        this.accountRepo = accountRepo;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Account>> getAllAccounts() {
        List<Account> accounts = accountRepo.findAll();
        accounts.sort(Comparator.comparing(Account::getId).reversed());
        return ResponseEntity.ok(accounts);
    }

    @GetMapping("/customer/{customerId}")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<List<Account>> getAccountsByCustomer(@PathVariable Long customerId) {
        List<Account> accounts = accountRepo.findByCustomerId(customerId);
        accounts.sort(Comparator.comparing(Account::getId).reversed());
        return ResponseEntity.ok(accounts);
    }
}