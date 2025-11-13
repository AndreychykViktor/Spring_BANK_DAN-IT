package com.example.hm1.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "accounts")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Account {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String number;

    @Enumerated(EnumType.STRING)
    private Currency currency;

    @Column(nullable = false)
    private Double balance = 0.0;

    @JsonBackReference
    @ManyToOne
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @CreationTimestamp
    @Column(name = "created_date", nullable = false, updatable = false)
    private LocalDateTime createdDate;

    public Account(Currency currency, Customer customer) {
        this.currency = currency;
        this.customer = customer;
        this.number = UUID.randomUUID().toString();
        this.balance = 0.0;
    }

    public Account(Long id, String number, Currency currency, Double balance, Customer customer) {
        this.id = id;
        this.number = number;
        this.currency = currency;
        this.balance = balance;
        this.customer = customer;
    }

    public Account() {
    }

    public static AccountBuilder builder() {
        return new AccountBuilder();
    }

    public Customer getCustomer() {
        return customer;
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

    public Long getId() {
        return this.id;
    }

    public String getNumber() {
        return this.number;
    }

    public Currency getCurrency() {
        return this.currency;
    }

    public Double getBalance() {
        return this.balance;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setNumber(String number) {
        this.number = number;
    }

    public void setCurrency(Currency currency) {
        this.currency = currency;
    }

    public void setBalance(Double balance) {
        this.balance = balance;
    }

    public void setCustomer(Customer customer) {
        this.customer = customer;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(LocalDateTime createdDate) {
        this.createdDate = createdDate;
    }

    public static class AccountBuilder {
        private Long id;
        private String number;
        private Currency currency;
        private Double balance;
        private Customer customer;

        AccountBuilder() {
        }

        public AccountBuilder id(Long id) {
            this.id = id;
            return this;
        }

        public AccountBuilder number(String number) {
            this.number = number;
            return this;
        }

        public AccountBuilder currency(Currency currency) {
            this.currency = currency;
            return this;
        }

        public AccountBuilder balance(Double balance) {
            this.balance = balance;
            return this;
        }

        public AccountBuilder customer(Customer customer) {
            this.customer = customer;
            return this;
        }

        public Account build() {
            return new Account(this.id, this.number, this.currency, this.balance, this.customer);
        }

        public String toString() {
            return "Account.AccountBuilder(id=" + this.id + ", number=" + this.number + ", currency=" + this.currency + ", balance=" + this.balance + ", customer=" + this.customer + ")";
        }
    }
}