
package com.example.hm1.service;

import com.example.hm1.dao.AccountRepo;
import com.example.hm1.dao.CustomerRepo;
import com.example.hm1.entity.Account;
import com.example.hm1.entity.Currency;
import com.example.hm1.entity.Customer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CustomerServiceImpl implements CustomerService {

    private final CustomerRepo customerRepo;
    private final AccountRepo accountRepo;

    @Autowired
    public CustomerServiceImpl(CustomerRepo customerRepo, AccountRepo accountRepo) {
        this.customerRepo = customerRepo;
        this.accountRepo = accountRepo;
    }

    @Override
    public Customer createCustomer(String name, String email, Integer age) {
        Customer customer = new Customer(name, email, age);
        return customerRepo.save(customer);
    }

    @Override
    public Customer updateCustomer(Customer customer) {
        return customerRepo.save(customer);
    }

    @Override
    public Customer getCustomerById(Long id) {
        Customer customer = customerRepo.getOne(id);
        if (customer != null) {
            List<Account> accounts = accountRepo.findByCustomerId(id);
            customer.setAccounts(accounts);
        }
        return customer;
    }

    @Override
    public List<Customer> getAllCustomers() {
        List<Customer> customers = customerRepo.findAll();
        for (Customer customer : customers) {
            List<Account> accounts = accountRepo.findByCustomerId(customer.getId());
            customer.setAccounts(accounts);
        }
        return customers;
    }

    @Override
    public boolean deleteCustomer(Long id) {
        List<Account> customerAccounts = accountRepo.findByCustomerId(id);
        for (Account account : customerAccounts) {
            accountRepo.delete(account);
        }
        return customerRepo.deleteById(id);
    }

    @Override
    public Account createAccountForCustomer(Long customerId, Currency currency) {
        Customer customer = customerRepo.getOne(customerId);
        if (customer == null) {
            throw new IllegalArgumentException("Customer not found with id: " + customerId);
        }

        Account account = new Account(currency, customer);
        return accountRepo.save(account);
    }

    @Override
    public boolean deleteAccountFromCustomer(Long customerId, Long accountId) {
        Customer customer = customerRepo.getOne(customerId);
        if (customer == null) {
            return false;
        }

        Account account = accountRepo.getOne(accountId);
        if (account == null || !customerId.equals(account.getCustomer().getId())) {
            return false;
        }

        return accountRepo.delete(account);
    }
}