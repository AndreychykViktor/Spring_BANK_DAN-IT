package com.example.hm1.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.Objects;
import java.util.UUID;

@Data
@NoArgsConstructor
public class Account {
    private Long id;
    private String number;
    private Currency currency;
    private Double balance = 0.0;
    private Customer customer;

    public Account(Currency currency, Customer customer) {
        this.currency = currency;
        this.customer = customer;
        this.number = UUID.randomUUID().toString();
        this.balance = 0.0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Account account = (Account) o;
        return Objects.equals(id, account.id) &&
               Objects.equals(number, account.number);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, number);
    }

    @Override
    public String toString() {
        return "Account{" +
               "id=" + id +
               ", number='" + number + '\'' +
               ", currency=" + currency +
               ", balance=" + balance +
               ", customer=" + (customer != null ? customer.getName() : "null") +
               '}';
    }
}