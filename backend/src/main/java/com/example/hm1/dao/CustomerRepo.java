package com.example.hm1.dao;

import com.example.hm1.entity.Customer;

public interface CustomerRepo extends Dao<Customer> {
    Customer findByEmail(String email);
}