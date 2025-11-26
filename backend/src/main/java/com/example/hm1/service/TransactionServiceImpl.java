package com.example.hm1.service;

import com.example.hm1.dao.TransactionRepository;
import com.example.hm1.dto.ExpenseStatisticsDTO;
import com.example.hm1.entity.Account;
import com.example.hm1.entity.Customer;
import com.example.hm1.entity.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    @Override
    @Transactional
    public Transaction updateTransactionCategory(Long transactionId, Transaction.ExpenseCategory category) {
        Transaction transaction = getTransactionById(transactionId);
        transaction.setCategory(category);
        return transactionRepository.save(transaction);
    }

    @Override
    @Transactional(readOnly = true)
    public ExpenseStatisticsDTO getExpenseStatisticsByMonth(Long customerId, int year, int month) {
        LocalDate startDate = LocalDate.of(year, month, 1);
        LocalDate endDate = startDate.plusMonths(1);
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atStartOfDay();

        List<Object[]> results = transactionRepository.getExpenseStatisticsByMonth(
                customerId, startDateTime, endDateTime);

        Map<Transaction.ExpenseCategory, BigDecimal> expensesByCategory = new HashMap<>();
        BigDecimal totalExpenses = BigDecimal.ZERO;

        for (Object[] result : results) {
            Transaction.ExpenseCategory category = (Transaction.ExpenseCategory) result[0];
            BigDecimal amount = (BigDecimal) result[1];
            expensesByCategory.put(category, amount);
            totalExpenses = totalExpenses.add(amount);
        }

        Map<Transaction.ExpenseCategory, Double> percentagesByCategory = new HashMap<>();
        if (totalExpenses.compareTo(BigDecimal.ZERO) > 0) {
            for (Map.Entry<Transaction.ExpenseCategory, BigDecimal> entry : expensesByCategory.entrySet()) {
                BigDecimal percentage = entry.getValue()
                        .divide(totalExpenses, 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100));
                percentagesByCategory.put(entry.getKey(), percentage.doubleValue());
            }
        }

        return new ExpenseStatisticsDTO(expensesByCategory, totalExpenses, percentagesByCategory);
    }

    @Override
    @Transactional(readOnly = true)
    public ExpenseStatisticsDTO getExpenseStatisticsByAccountAndMonth(Long accountId, int year, int month) {
        LocalDate startDate = LocalDate.of(year, month, 1);
        LocalDate endDate = startDate.plusMonths(1);
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atStartOfDay();

        List<Object[]> results = transactionRepository.getExpenseStatisticsByAccountAndMonth(
                accountId, startDateTime, endDateTime);

        Map<Transaction.ExpenseCategory, BigDecimal> expensesByCategory = new HashMap<>();
        BigDecimal totalExpenses = BigDecimal.ZERO;

        for (Object[] result : results) {
            Transaction.ExpenseCategory category = (Transaction.ExpenseCategory) result[0];
            BigDecimal amount = (BigDecimal) result[1];
            expensesByCategory.put(category, amount);
            totalExpenses = totalExpenses.add(amount);
        }

        Map<Transaction.ExpenseCategory, Double> percentagesByCategory = new HashMap<>();
        if (totalExpenses.compareTo(BigDecimal.ZERO) > 0) {
            for (Map.Entry<Transaction.ExpenseCategory, BigDecimal> entry : expensesByCategory.entrySet()) {
                BigDecimal percentage = entry.getValue()
                        .divide(totalExpenses, 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100));
                percentagesByCategory.put(entry.getKey(), percentage.doubleValue());
            }
        }

        return new ExpenseStatisticsDTO(expensesByCategory, totalExpenses, percentagesByCategory);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Long, ExpenseStatisticsDTO> getExpenseStatisticsForAllUsersByMonth(int year, int month) {
        LocalDate startDate = LocalDate.of(year, month, 1);
        LocalDate endDate = startDate.plusMonths(1);
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atStartOfDay();

        List<Object[]> results = transactionRepository.getExpenseStatisticsForAllUsersByMonth(
                startDateTime, endDateTime);

        Map<Long, Map<Transaction.ExpenseCategory, BigDecimal>> userExpensesMap = new HashMap<>();

        for (Object[] result : results) {
            Long customerId = ((Number) result[0]).longValue();
            Transaction.ExpenseCategory category = (Transaction.ExpenseCategory) result[1];
            BigDecimal amount = (BigDecimal) result[2];

            userExpensesMap.computeIfAbsent(customerId, k -> new HashMap<>())
                    .put(category, amount);
        }

        Map<Long, ExpenseStatisticsDTO> statisticsByUser = new HashMap<>();

        for (Map.Entry<Long, Map<Transaction.ExpenseCategory, BigDecimal>> entry : userExpensesMap.entrySet()) {
            Long customerId = entry.getKey();
            Map<Transaction.ExpenseCategory, BigDecimal> expensesByCategory = entry.getValue();
            BigDecimal totalExpenses = expensesByCategory.values().stream()
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            Map<Transaction.ExpenseCategory, Double> percentagesByCategory = new HashMap<>();
            if (totalExpenses.compareTo(BigDecimal.ZERO) > 0) {
                for (Map.Entry<Transaction.ExpenseCategory, BigDecimal> catEntry : expensesByCategory.entrySet()) {
                    BigDecimal percentage = catEntry.getValue()
                            .divide(totalExpenses, 4, RoundingMode.HALF_UP)
                            .multiply(BigDecimal.valueOf(100));
                    percentagesByCategory.put(catEntry.getKey(), percentage.doubleValue());
                }
            }

            statisticsByUser.put(customerId, new ExpenseStatisticsDTO(
                    expensesByCategory, totalExpenses, percentagesByCategory));
        }

        return statisticsByUser;
    }
}
