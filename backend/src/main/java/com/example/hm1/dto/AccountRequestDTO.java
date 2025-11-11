package com.example.hm1.dto;

import com.example.hm1.entity.Currency;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public class AccountRequestDTO {
    
    @NotNull(message = "Валюта обов'язкова")
    private Currency currency;
    
    @DecimalMin(value = "0.0", inclusive = true, message = "Баланс не може бути негативним")
    @Digits(integer = 15, fraction = 2, message = "Баланс має бути числом з максимум 2 знаками після коми")
    private BigDecimal balance;

    public AccountRequestDTO() {
    }

    public Currency getCurrency() {
        return currency;
    }

    public void setCurrency(Currency currency) {
        this.currency = currency;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }
}

