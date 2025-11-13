
package com.example.hm1.controller;

import com.example.hm1.dao.AccountRepo;
import com.example.hm1.dto.AccountOperationDTO;
import com.example.hm1.dto.AccountResponseDTO;
import com.example.hm1.dto.TransferDTO;
import com.example.hm1.entity.Account;
import com.example.hm1.service.AccountService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/accounts")
@CrossOrigin(origins = "*")
public class AccountController {

    private final AccountRepo accountRepo;
    private final AccountService accountService;

    public AccountController(AccountRepo accountRepo, AccountService accountService) {
        this.accountRepo = accountRepo;
        this.accountService = accountService;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<AccountResponseDTO>> getAllAccounts() {
        List<Account> accounts = accountRepo.findAll();
        accounts.sort(Comparator.comparing(Account::getId).reversed());
        List<AccountResponseDTO> response = accounts.stream()
                .map(AccountResponseDTO::from)
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/customer/{customerId}")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<List<AccountResponseDTO>> getAccountsByCustomer(@PathVariable Long customerId) {
        List<Account> accounts = accountRepo.findByCustomerId(customerId);
        accounts.sort(Comparator.comparing(Account::getId).reversed());
        List<AccountResponseDTO> response = accounts.stream()
                .map(AccountResponseDTO::from)
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{accountNumber}/deposit")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<?> deposit(@PathVariable String accountNumber, @Valid @RequestBody AccountOperationDTO dto) {
        try {
            Double amount = dto.getAmount().doubleValue();
            System.out.println("AccountController.deposit: accountNumber=" + accountNumber + ", amount=" + amount);
            
            Account account = accountRepo.findByNumber(accountNumber);
            if (account == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Account not found");
            }
            
            accountService.deposit(accountNumber, amount);
            return ResponseEntity.ok("Deposit successful");
        } catch (Exception e) {
            System.err.println("AccountController.deposit: Error: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Error: " + e.getMessage());
        }
    }

    @PostMapping("/{accountNumber}/withdraw")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<?> withdraw(@PathVariable String accountNumber, @Valid @RequestBody AccountOperationDTO dto) {
        try {
            Double amount = dto.getAmount().doubleValue();
            System.out.println("AccountController.withdraw: accountNumber=" + accountNumber + ", amount=" + amount);
            
            Account account = accountRepo.findByNumber(accountNumber);
            if (account == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Account not found");
            }
            
            accountService.withdraw(accountNumber, amount);
            return ResponseEntity.ok("Withdrawal successful");
        } catch (Exception e) {
            System.err.println("AccountController.withdraw: Error: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Error: " + e.getMessage());
        }
    }

    @PostMapping("/{fromAccountNumber}/transfer")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<?> transfer(@PathVariable String fromAccountNumber, @Valid @RequestBody TransferDTO dto) {
        try {
            String toAccountNumber = dto.getToAccountNumber();
            Double amount = dto.getAmount().doubleValue();
            System.out.println("AccountController.transfer: from=" + fromAccountNumber + ", to=" + toAccountNumber + ", amount=" + amount);
            
            Account fromAccount = accountRepo.findByNumber(fromAccountNumber);
            if (fromAccount == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Source account not found");
            }
            
            Account toAccount = accountRepo.findByNumber(toAccountNumber);
            if (toAccount == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Destination account not found");
            }
            
            accountService.transfer(fromAccountNumber, toAccountNumber, amount);
            return ResponseEntity.ok("Transfer successful");
        } catch (Exception e) {
            System.err.println("AccountController.transfer: Error: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Error: " + e.getMessage());
        }
    }
}