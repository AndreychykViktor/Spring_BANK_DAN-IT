package com.example.hm1.dao;

import com.example.hm1.entity.Account;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Repository
public class AccountRepoImpl implements AccountRepo {
    private final List<Account> accounts = new ArrayList<>();
    private final AtomicLong idGenerator = new AtomicLong(1);

    @Override
    public Account save(Account account) {
        if (account.getId() == null) {
            account.setId(idGenerator.getAndIncrement());
        }

        Account existing = getOne(account.getId());
        if (existing != null) {
            accounts.remove(existing);
        }

        accounts.add(account);
        return account;
    }

    @Override
    public boolean delete(Account account) {
        return accounts.remove(account);
    }

    @Override
    public void deleteAll(List<Account> entities) {
        accounts.removeAll(entities);
    }

    @Override
    public void saveAll(List<Account> entities) {
        for (Account account : entities) {
            save(account);
        }
    }

    @Override
    public List<Account> findAll() {
        return new ArrayList<>(accounts);
    }

    @Override
    public boolean deleteById(long id) {
        return accounts.removeIf(account -> account.getId() != null && account.getId() == id);
    }

    @Override
    public Account getOne(long id) {
        return accounts.stream()
                .filter(account -> account.getId() != null && account.getId() == id)
                .findFirst()
                .orElse(null);
    }

    @Override
    public Account findByNumber(String number) {
        return accounts.stream()
                .filter(account -> number.equals(account.getNumber()))
                .findFirst()
                .orElse(null);
    }

    @Override
    public List<Account> findByCustomerId(Long customerId) {
        return accounts.stream()
                .filter(account -> account.getCustomer() != null &&
                                   customerId.equals(account.getCustomer().getId()))
                .collect(Collectors.toList());
    }
}