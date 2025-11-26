package com.example.hm1.controller;

import com.example.hm1.dto.ExpenseStatisticsDTO;
import com.example.hm1.dto.UpdateTransactionCategoryDTO;
import com.example.hm1.entity.Transaction;
import com.example.hm1.service.TransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/transactions")
@CrossOrigin(origins = "*")
@Tag(name = "Transactions", description = "API для управління транзакціями та статистикою витрат")
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @GetMapping("/customer/{customerId}")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    @Operation(summary = "Отримати транзакції клієнта", description = "Повертає список всіх транзакцій вказаного клієнта")
    @ApiResponse(responseCode = "200", description = "Успішно отримано список транзакцій")
    public ResponseEntity<List<Transaction>> getTransactionsByCustomer(
            @Parameter(description = "ID клієнта", required = true)
            @PathVariable Long customerId) {
        System.out.println("TransactionController.getTransactionsByCustomer: customerId=" + customerId);
        List<Transaction> transactions = transactionService.getTransactionsByCustomerId(customerId);
        System.out.println("TransactionController.getTransactionsByCustomer: Found " + transactions.size() + " transactions");
        return ResponseEntity.ok(transactions);
    }

    @GetMapping("/history/{customerId}")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<List<Transaction>> getTransactionHistory(@PathVariable Long customerId) {
        System.out.println("TransactionController.getTransactionHistory: customerId=" + customerId);
        List<Transaction> transactions = transactionService.getTransactionsByCustomerId(customerId);
        System.out.println("TransactionController.getTransactionHistory: Found " + transactions.size() + " transactions");
        return ResponseEntity.ok(transactions);
    }

    @GetMapping("/customer/{customerId}/page")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<Page<Transaction>> getTransactionsByCustomerPage(
            @PathVariable Long customerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Transaction> transactions = transactionService.getTransactionsByCustomerId(customerId, pageable);
        return ResponseEntity.ok(transactions);
    }

    @GetMapping("/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Transaction>> getAllTransactions() {
        List<Transaction> transactions = transactionService.getAllTransactions();
        return ResponseEntity.ok(transactions);
    }

    @GetMapping("/all/page")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<Transaction>> getAllTransactionsPage(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Transaction> transactions = transactionService.getAllTransactions(pageable);
        return ResponseEntity.ok(transactions);
    }

    @GetMapping("/customer/{customerId}/date-range")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<List<Transaction>> getTransactionsByDateRange(
            @PathVariable Long customerId,
            @RequestParam String startDate,
            @RequestParam String endDate) {
        LocalDateTime start = LocalDateTime.parse(startDate);
        LocalDateTime end = LocalDateTime.parse(endDate);
        List<Transaction> transactions = transactionService.getTransactionsByDateRange(customerId, start, end);
        return ResponseEntity.ok(transactions);
    }

    @GetMapping("/all/date-range")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Transaction>> getAllTransactionsByDateRange(
            @RequestParam String startDate,
            @RequestParam String endDate) {
        LocalDateTime start = LocalDateTime.parse(startDate);
        LocalDateTime end = LocalDateTime.parse(endDate);
        List<Transaction> transactions = transactionService.getAllTransactionsByDateRange(start, end);
        return ResponseEntity.ok(transactions);
    }

    @GetMapping("/type/{type}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Transaction>> getTransactionsByType(@PathVariable Transaction.TransactionType type) {
        List<Transaction> transactions = transactionService.getTransactionsByType(type);
        return ResponseEntity.ok(transactions);
    }

    @GetMapping("/status/{status}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Transaction>> getTransactionsByStatus(@PathVariable Transaction.TransactionStatus status) {
        List<Transaction> transactions = transactionService.getTransactionsByStatus(status);
        return ResponseEntity.ok(transactions);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<Transaction> getTransactionById(@PathVariable Long id) {
        Transaction transaction = transactionService.getTransactionById(id);
        return ResponseEntity.ok(transaction);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteTransaction(@PathVariable Long id) {
        transactionService.deleteTransaction(id);
        return ResponseEntity.ok("Transaction deleted successfully");
    }

    @PutMapping("/{id}/category")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    @Operation(summary = "Оновити категорію транзакції", description = "Додає або змінює категорію витрат для транзакції (FOOD, TRANSPORT, SHOPPING, тощо)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Категорію успішно оновлено"),
            @ApiResponse(responseCode = "404", description = "Транзакція не знайдена")
    })
    public ResponseEntity<Transaction> updateTransactionCategory(
            @Parameter(description = "ID транзакції", required = true)
            @PathVariable Long id,
            @Parameter(description = "Нова категорія транзакції (може бути null для видалення)", required = true)
            @RequestBody UpdateTransactionCategoryDTO dto) {
        // Allow null category to remove category assignment
        Transaction updated = transactionService.updateTransactionCategory(id, dto.getCategory());
        return ResponseEntity.ok(updated);
    }

    @GetMapping("/customer/{customerId}/expense-statistics")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    @Operation(summary = "Отримати статистику витрат клієнта", description = "Повертає статистику витрат клієнта за вказаний місяць з розподілом по категоріях")
    @ApiResponse(responseCode = "200", description = "Успішно отримано статистику витрат")
    public ResponseEntity<ExpenseStatisticsDTO> getExpenseStatistics(
            @Parameter(description = "ID клієнта", required = true)
            @PathVariable Long customerId,
            @Parameter(description = "Рік", required = true)
            @RequestParam int year,
            @Parameter(description = "Місяць (1-12)", required = true)
            @RequestParam int month) {
        ExpenseStatisticsDTO statistics = transactionService.getExpenseStatisticsByMonth(customerId, year, month);
        return ResponseEntity.ok(statistics);
    }

    @GetMapping("/account/{accountId}/expense-statistics")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<ExpenseStatisticsDTO> getExpenseStatisticsByAccount(
            @PathVariable Long accountId,
            @RequestParam int year,
            @RequestParam int month) {
        ExpenseStatisticsDTO statistics = transactionService.getExpenseStatisticsByAccountAndMonth(accountId, year, month);
        return ResponseEntity.ok(statistics);
    }

    @GetMapping("/all/expense-statistics")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<Long, ExpenseStatisticsDTO>> getExpenseStatisticsForAllUsers(
            @RequestParam int year,
            @RequestParam int month) {
        Map<Long, ExpenseStatisticsDTO> statistics = transactionService.getExpenseStatisticsForAllUsersByMonth(year, month);
        return ResponseEntity.ok(statistics);
    }
}
