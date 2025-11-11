package com.example.hm1.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public class TransferDTO {
    
    @NotBlank(message = "Номер рахунку отримувача обов'язковий")
    private String toAccountNumber;
    
    @NotNull(message = "Сума обов'язкова")
    @DecimalMin(value = "0.01", message = "Сума має бути більше 0")
    @Digits(integer = 15, fraction = 2, message = "Сума має бути числом з максимум 2 знаками після коми")
    private BigDecimal amount;

    public TransferDTO() {
    }

    public String getToAccountNumber() {
        return toAccountNumber;
    }

    public void setToAccountNumber(String toAccountNumber) {
        this.toAccountNumber = toAccountNumber;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }
}

