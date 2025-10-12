package com.example.hm1.dao;

import com.example.hm1.entity.Account;
import java.util.List;

public interface AccountRepo extends Dao<Account> {
    Account findByNumber(String number);
    List<Account> findByCustomerId(Long customerId);
}