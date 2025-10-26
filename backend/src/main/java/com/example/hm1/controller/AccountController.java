
package com.example.hm1.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.example.hm1.entity.Account;
import com.example.hm1.service.AccountService;

import java.util.Map;

@RestController
@RequestMapping("/api/accounts")

@CrossOrigin(origins = "*")
public class AccountController {

    private final AccountService accountService;
    @Autowired
    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @PostMapping("/{accountNumber}/deposit")
    public ResponseEntity<String> deposit(
            @PathVariable String accountNumber,
            @RequestBody Map<String, Double> request) {
        Double amount = request.get("amount");

        if (amount == null) {
            return ResponseEntity.badRequest().body("Amount is required");
        }

        boolean success = accountService.deposit(accountNumber, amount);
        if (success) {
            return ResponseEntity.ok("Deposit successful");
        }
        return ResponseEntity.badRequest().body("Deposit failed");
    }

    @PostMapping("/{accountNumber}/withdraw")
    public ResponseEntity<String> withdraw(
            @PathVariable String accountNumber,
            @RequestBody Map<String, Double> request) {
        Double amount = request.get("amount");
        
        if (amount == null) {
            return ResponseEntity.badRequest().body("Amount is required");
        }

        boolean success = accountService.withdraw(accountNumber, amount);
        if (success) {
            return ResponseEntity.ok("Withdrawal successful");
        }
        return ResponseEntity.badRequest().body("Insufficient funds or account not found");
    }

    @PostMapping("/transfer")
    public ResponseEntity<String> transfer(@RequestBody Map<String, Object> request) {
        String fromAccount = (String) request.get("fromAccount");
        String toAccount = (String) request.get("toAccount");
        Double amount = (Double) request.get("amount");

        if (fromAccount == null || toAccount == null || amount == null) {
            return ResponseEntity.badRequest().body("From account, to account, and amount are required");
        }

        boolean success = accountService.transfer(fromAccount, toAccount, amount);
        if (success) {
            return ResponseEntity.ok("Transfer successful");
        }
        return ResponseEntity.badRequest().body("Transfer failed - insufficient funds or account not found");
    }

    @GetMapping("/{accountNumber}")
    public ResponseEntity<Account> getAccount(@PathVariable String accountNumber) {
        Account account = accountService.getAccountByNumber(accountNumber);
        if (account == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(account);
    }
}