
package com.example.hm1.controller;

import com.example.hm1.dao.AccountRepo;
import com.example.hm1.dto.AccountOperationDTO;
import com.example.hm1.dto.AccountResponseDTO;
import com.example.hm1.dto.TransferDTO;
import com.example.hm1.entity.Account;
import com.example.hm1.service.AccountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Accounts", description = "API для управління банківськими рахунками та операціями з ними")
public class AccountController {

    private final AccountRepo accountRepo;
    private final AccountService accountService;

    public AccountController(AccountRepo accountRepo, AccountService accountService) {
        this.accountRepo = accountRepo;
        this.accountService = accountService;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Отримати всі рахунки", description = "Повертає список всіх банківських рахунків (тільки для адміністраторів)")
    @ApiResponse(responseCode = "200", description = "Успішно отримано список рахунків")
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
    @Operation(summary = "Поповнити рахунок", description = "Додає вказану суму до балансу рахунку")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Рахунок успішно поповнено"),
            @ApiResponse(responseCode = "400", description = "Помилка: невалідні дані або рахунок не знайдено"),
            @ApiResponse(responseCode = "404", description = "Рахунок з вказаним номером не знайдено")
    })
    public ResponseEntity<?> deposit(
            @Parameter(description = "Номер рахунку", required = true)
            @PathVariable String accountNumber,
            @Parameter(description = "Дані операції: amount (сума для поповнення)", required = true)
            @Valid @RequestBody AccountOperationDTO dto) {
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
    @Operation(summary = "Зняти кошти з рахунку", description = "Знімає вказану суму з балансу рахунку (з перевіркою достатності коштів)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Кошти успішно знято з рахунку"),
            @ApiResponse(responseCode = "400", description = "Помилка: недостатньо коштів на рахунку або невалідні дані"),
            @ApiResponse(responseCode = "404", description = "Рахунок з вказаним номером не знайдено")
    })
    public ResponseEntity<?> withdraw(
            @Parameter(description = "Номер рахунку", required = true)
            @PathVariable String accountNumber,
            @Parameter(description = "Дані операції: amount (сума для зняття)", required = true)
            @Valid @RequestBody AccountOperationDTO dto) {
        try {
            Double amount = dto.getAmount().doubleValue();
            System.out.println("AccountController.withdraw: accountNumber=" + accountNumber + ", amount=" + amount);
            
            Account account = accountRepo.findByNumber(accountNumber);
            if (account == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Account not found");
            }
            
            // Перевірка балансу перед зняттям
            if (account.getBalance() < amount) {
                String balanceFormatted = String.format("%.2f", account.getBalance());
                String amountFormatted = String.format("%.2f", amount);
                String currency = account.getCurrency().name();
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Недостатньо коштів на рахунку. Доступний баланс: " + balanceFormatted + " " + currency + 
                          ", потрібно: " + amountFormatted + " " + currency);
            }
            
            boolean success = accountService.withdraw(accountNumber, amount);
            if (!success) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Помилка виконання операції зняття коштів.");
            }
            
            return ResponseEntity.ok("Withdrawal successful");
        } catch (Exception e) {
            System.err.println("AccountController.withdraw: Error: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Error: " + e.getMessage());
        }
    }

    @PostMapping("/{fromAccountNumber}/transfer")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    @Operation(summary = "Переказ коштів між рахунками", description = "Переводить вказану суму з одного рахунку на інший з автоматичним конвертуванням валют при необхідності")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Переказ виконано успішно"),
            @ApiResponse(responseCode = "400", description = "Помилка: недостатньо коштів на рахунку відправника або невалідні дані"),
            @ApiResponse(responseCode = "404", description = "Вихідний або цільовий рахунок не знайдено")
    })
    public ResponseEntity<?> transfer(
            @Parameter(description = "Номер рахунку відправника", required = true)
            @PathVariable String fromAccountNumber,
            @Parameter(description = "Дані переказу: toAccountNumber (номер рахунку отримувача), amount (сума)", required = true)
            @Valid @RequestBody TransferDTO dto) {
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
            
            // Перевірка балансу перед переказом
            if (fromAccount.getBalance() < amount) {
                String balanceFormatted = String.format("%.2f", fromAccount.getBalance());
                String amountFormatted = String.format("%.2f", amount);
                String currency = fromAccount.getCurrency().name();
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Недостатньо коштів на рахунку. Доступний баланс: " + balanceFormatted + " " + currency + 
                          ", потрібно: " + amountFormatted + " " + currency);
            }
            
            boolean success = accountService.transfer(fromAccountNumber, toAccountNumber, amount);
            if (!success) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Помилка виконання переказу. Перевірте баланс та правильність даних.");
            }
            
            return ResponseEntity.ok("Transfer successful");
        } catch (Exception e) {
            System.err.println("AccountController.transfer: Error: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Error: " + e.getMessage());
        }
    }
}