package com.example.hm1.dao;

import com.example.hm1.entity.Customer;
import com.example.hm1.entity.User;

import java.util.Optional;

public interface CustomerRepo extends Dao<Customer> {
    Customer findByEmail(String email);
    Optional<Customer> findByUser(User user);
}