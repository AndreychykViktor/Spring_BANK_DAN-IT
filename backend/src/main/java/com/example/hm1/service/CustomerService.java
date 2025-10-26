package com.example.hm1.service;

import com.example.hm1.entity.Account;
import com.example.hm1.entity.Currency;
import com.example.hm1.entity.Customer;

import java.util.List;

public interface CustomerService {
    Customer createCustomer(String name, Integer age);
    Customer updateCustomer(Customer customer);
    Customer getCustomerById(Long id);
    List<Customer> getAllCustomers();
    boolean deleteCustomer(Long id);
    Account createAccountForCustomer(Long customerId, Currency currency, String email, String password);
    boolean deleteAccountFromCustomer(Long customerId, Long accountId);
}