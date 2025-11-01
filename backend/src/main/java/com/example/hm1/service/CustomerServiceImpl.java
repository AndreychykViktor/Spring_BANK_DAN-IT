
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
    public Customer createCustomer(String name, Integer age) {
        Customer customer = new Customer(name, "temp@email.com", age);
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
    public Account createAccountForCustomer(Long customerId, Currency currency, String email, String password) {
        System.out.println("CustomerServiceImpl.createAccountForCustomer: customerId=" + customerId + ", currency=" + currency);
        Customer customer = customerRepo.getOne(customerId);
        if (customer == null) {
            System.err.println("CustomerServiceImpl.createAccountForCustomer: Customer not found with id: " + customerId);
            throw new IllegalArgumentException("Customer not found with id: " + customerId);
        }

        System.out.println("CustomerServiceImpl.createAccountForCustomer: Found customer: " + customer.getName());
        
        // Генеруємо номер акаунту в форматі ACC-XXXX
        String accountNumber = generateAccountNumber();
        System.out.println("CustomerServiceImpl.createAccountForCustomer: Generated account number: " + accountNumber);
        
        Account account = new Account(currency, customer);
        account.setNumber(accountNumber);
        Account saved = accountRepo.save(account);
        System.out.println("CustomerServiceImpl.createAccountForCustomer: Account created successfully. ID=" + saved.getId() + ", Number=" + saved.getNumber());
        return saved;
    }
    
    private String generateAccountNumber() {
        String lastNumber = accountRepo.getLastAccountNumber();
        int nextNumber = 1;
        
        if (lastNumber != null && lastNumber.startsWith("ACC-")) {
            try {
                String numberPart = lastNumber.substring(4); // Вирізаємо "ACC-"
                nextNumber = Integer.parseInt(numberPart) + 1;
            } catch (NumberFormatException e) {
                System.err.println("CustomerServiceImpl.generateAccountNumber: Failed to parse last number: " + lastNumber);
                nextNumber = 1;
            }
        }
        
        // Форматуємо з ведучими нулями: ACC-0001, ACC-0002, ..., ACC-9999
        return String.format("ACC-%04d", nextNumber);
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