package com.example.hm1.dao;

import com.example.hm1.entity.Account;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@Primary
public class AccountRepoJpaImpl implements AccountRepo {

    private final AccountJpaRepository jpa;

    public AccountRepoJpaImpl(AccountJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Account save(Account obj) {
        return jpa.save(obj);
    }

    @Override
    public boolean delete(Account obj) {
        jpa.delete(obj);
        return true;
    }

    @Override
    public void deleteAll(List<Account> entities) {
        jpa.deleteAll(entities);
    }

    @Override
    public void saveAll(List<Account> entities) {
        jpa.saveAll(entities);
    }

    @Override
    public List<Account> findAll() {
        return jpa.findAll();
    }

    @Override
    public boolean deleteById(long id) {
        if (jpa.existsById(id)) {
            jpa.deleteById(id);
            return true;
        }
        return false;
    }

    @Override
    public Account getOne(long id) {
        return jpa.findById(id).orElse(null);
    }

    @Override
    public Account findByNumber(String number) {
        return jpa.findByNumber(number);
    }

    @Override
    public List<Account> findByCustomerId(Long customerId) {
        return jpa.findByCustomer_Id(customerId);
    }

    @Override
    public String getLastAccountNumber() {
        List<String> numbers = jpa.findAllAccountNumbersOrderedDesc();
        if (numbers.isEmpty()) {
            return null;
        }
        return numbers.get(0);
    }
}
