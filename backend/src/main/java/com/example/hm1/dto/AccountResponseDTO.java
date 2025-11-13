package com.example.hm1.dto;

import com.example.hm1.entity.Account;
import com.example.hm1.entity.Currency;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

public class AccountResponseDTO {

    private Long id;
    private String number;
    private Currency currency;
    private Double balance;
    private Long customerId;
    private String customerName;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdDate;

    public AccountResponseDTO() {
    }

    public AccountResponseDTO(Long id,
                              String number,
                              Currency currency,
                              Double balance,
                              Long customerId,
                              String customerName,
                              LocalDateTime createdDate) {
        this.id = id;
        this.number = number;
        this.currency = currency;
        this.balance = balance;
        this.customerId = customerId;
        this.customerName = customerName;
        this.createdDate = createdDate;
    }

    public static AccountResponseDTO from(Account account) {
        if (account == null) {
            return null;
        }

        Long ownerId = account.getCustomer() != null ? account.getCustomer().getId() : null;
        String ownerName = account.getCustomer() != null ? account.getCustomer().getName() : null;

        return new AccountResponseDTO(
                account.getId(),
                account.getNumber(),
                account.getCurrency(),
                account.getBalance(),
                ownerId,
                ownerName,
                account.getCreatedDate()
        );
    }

    public Long getId() {
        return id;
    }

    public String getNumber() {
        return number;
    }

    public Currency getCurrency() {
        return currency;
    }

    public Double getBalance() {
        return balance;
    }

    public Long getCustomerId() {
        return customerId;
    }

    public String getCustomerName() {
        return customerName;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }
}

