package com.example.hm1.controller;

import com.example.hm1.dao.CustomerRepo;
import com.example.hm1.dao.UserRepository;
import com.example.hm1.entity.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import com.example.hm1.entity.Account;
import com.example.hm1.entity.Currency;
import com.example.hm1.entity.Customer;
import com.example.hm1.service.CustomerService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/customers")
@CrossOrigin(origins = "*")
public class CustomerController {

    private final CustomerService customerService;
    private final CustomerRepo customerRepo;
    private final UserRepository userRepository;

    @Autowired
    public CustomerController(CustomerService customerService, CustomerRepo customerRepo, UserRepository userRepository) {
        this.customerService = customerService;
        this.customerRepo = customerRepo;
        this.userRepository = userRepository;
    }

    @GetMapping
    public ResponseEntity<List<Customer>> getAllCustomers() {
        List<Customer> customers = customerService.getAllCustomers();
        return ResponseEntity.ok(customers);
    }

    @GetMapping("/me")
    public ResponseEntity<Customer> getCurrentCustomer() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String username = authentication.getName();
            
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            
            Customer customer = customerRepo.findByUser(user)
                    .orElse(null);
            
            if (customer == null) {
                return ResponseEntity.notFound().build();
            }
            
            return ResponseEntity.ok(customer);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<Customer> getCustomerById(@PathVariable Long id) {
        Customer customer = customerService.getCustomerById(id);
        if (customer == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(customer);
    }

    @PostMapping
    public ResponseEntity<Customer> createCustomer(@RequestBody Map<String, Object> customerData) {
        try {
            String name = (String) customerData.get("name");
            String email = (String) customerData.get("email");
            Integer age = (Integer) customerData.get("age");

            Customer customer = customerService.createCustomer(name, age);
            return ResponseEntity.status(HttpStatus.CREATED).body(customer);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<Customer> updateCustomer(@PathVariable Long id, @RequestBody Customer customer) {
        Customer existingCustomer = customerService.getCustomerById(id);
        if (existingCustomer == null) {
            return ResponseEntity.notFound().build();
        }

        customer.setId(id);
        Customer updatedCustomer = customerService.updateCustomer(customer);
        return ResponseEntity.ok(updatedCustomer);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCustomer(@PathVariable Long id) {
        boolean deleted = customerService.deleteCustomer(id);
        if (deleted) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }

    @PostMapping("/{customerId}/accounts")
    public ResponseEntity<Account> createAccountForCustomer(
            @PathVariable Long customerId,
            @RequestBody Map<String, String> requestBody) {
        try {
            Currency currency = Currency.valueOf(requestBody.get("currency"));
            Account account = customerService.createAccountForCustomer(customerId, currency, requestBody.get("email"), requestBody.get("password"));
            return ResponseEntity.status(HttpStatus.CREATED).body(account);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @DeleteMapping("/{customerId}/accounts/{accountId}")
    public ResponseEntity<Void> deleteAccountFromCustomer(
            @PathVariable Long customerId,
            @PathVariable Long accountId) {
        boolean deleted = customerService.deleteAccountFromCustomer(customerId, accountId);
        if (deleted) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }
}