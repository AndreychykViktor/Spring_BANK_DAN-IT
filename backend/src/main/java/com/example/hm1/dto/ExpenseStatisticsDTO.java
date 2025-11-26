package com.example.hm1.dto;

import com.example.hm1.entity.Transaction;

import java.math.BigDecimal;
import java.util.Map;

public class ExpenseStatisticsDTO {
    private Map<Transaction.ExpenseCategory, BigDecimal> expensesByCategory;
    private BigDecimal totalExpenses;
    private Map<Transaction.ExpenseCategory, Double> percentagesByCategory;

    public ExpenseStatisticsDTO() {
    }

    public ExpenseStatisticsDTO(Map<Transaction.ExpenseCategory, BigDecimal> expensesByCategory, 
                                BigDecimal totalExpenses,
                                Map<Transaction.ExpenseCategory, Double> percentagesByCategory) {
        this.expensesByCategory = expensesByCategory;
        this.totalExpenses = totalExpenses;
        this.percentagesByCategory = percentagesByCategory;
    }

    public Map<Transaction.ExpenseCategory, BigDecimal> getExpensesByCategory() {
        return expensesByCategory;
    }

    public void setExpensesByCategory(Map<Transaction.ExpenseCategory, BigDecimal> expensesByCategory) {
        this.expensesByCategory = expensesByCategory;
    }

    public BigDecimal getTotalExpenses() {
        return totalExpenses;
    }

    public void setTotalExpenses(BigDecimal totalExpenses) {
        this.totalExpenses = totalExpenses;
    }

    public Map<Transaction.ExpenseCategory, Double> getPercentagesByCategory() {
        return percentagesByCategory;
    }

    public void setPercentagesByCategory(Map<Transaction.ExpenseCategory, Double> percentagesByCategory) {
        this.percentagesByCategory = percentagesByCategory;
    }
}
