package com.example.hm1.dto;

import com.example.hm1.entity.Transaction;

public class UpdateTransactionCategoryDTO {
    private Transaction.ExpenseCategory category;

    public UpdateTransactionCategoryDTO() {
    }

    public UpdateTransactionCategoryDTO(Transaction.ExpenseCategory category) {
        this.category = category;
    }

    public Transaction.ExpenseCategory getCategory() {
        return category;
    }

    public void setCategory(Transaction.ExpenseCategory category) {
        this.category = category;
    }
}
