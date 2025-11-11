package com.example.hm1.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public class AccountOperationDTO {
    
    @NotNull(message = "Сума обов'язкова")
    @DecimalMin(value = "0.01", message = "Сума має бути більше 0")
    @Digits(integer = 15, fraction = 2, message = "Сума має бути числом з максимум 2 знаками після коми")
    private BigDecimal amount;

    public AccountOperationDTO() {
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }
}

