package com.example.hm1.controller;

import com.example.hm1.dao.CustomerRepo;
import com.example.hm1.dao.UserRepository;
import com.example.hm1.dao.EmployerRepo;
import com.example.hm1.entity.User;
import com.example.hm1.entity.Employer;
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

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/customers")
@CrossOrigin(origins = "*")
public class CustomerController {

    private final CustomerService customerService;
    private final CustomerRepo customerRepo;
    private final UserRepository userRepository;
    private final EmployerRepo employerRepo;

    @Autowired
    public CustomerController(CustomerService customerService, CustomerRepo customerRepo, UserRepository userRepository, EmployerRepo employerRepo) {
        this.customerService = customerService;
        this.customerRepo = customerRepo;
        this.userRepository = userRepository;
        this.employerRepo = employerRepo;
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
            
            if (username == null) {
                System.err.println("CustomerController: username is null");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
            
            System.out.println("CustomerController: Looking for customer for username: " + username);
            
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found: " + username));
            
            if (user.getId() == null) {
                System.err.println("CustomerController: User ID is null for username: " + username);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
            }
            
            System.out.println("CustomerController: Found user with ID: " + user.getId());
            
            // Використовуємо findByUserId для прямого пошуку за ID
            Customer customer = customerRepo.findByUserId(user.getId())
                    .orElse(null);
            
            if (customer == null) {
                System.err.println("CustomerController: Customer not found for userId: " + user.getId());
                System.err.println("CustomerController: Total customers in repo: " + customerRepo.findAll().size());
                // Логуємо всіх клієнтів для діагностики
                customerRepo.findAll().forEach(c -> {
                    System.err.println("  Customer ID: " + c.getId() + ", name: " + c.getName() + 
                                     ", user ID: " + (c.getUser() != null ? c.getUser().getId() : "null"));
                });
                // Якщо не знайдено, повертаємо 404
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }
            
            System.out.println("CustomerController: Found customer with ID: " + customer.getId() + 
                             ", name: " + customer.getName());
            return ResponseEntity.ok(customer);
        } catch (RuntimeException e) {
            System.err.println("CustomerController: RuntimeException: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (Exception e) {
            System.err.println("CustomerController: Exception: " + e.getMessage());
            e.printStackTrace();
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
            Integer age = (Integer) customerData.get("age");

            Customer customer = customerService.createCustomer(name, age);
            return ResponseEntity.status(HttpStatus.CREATED).body(customer);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/me")
    public ResponseEntity<?> updateCurrentCustomer(@RequestBody Map<String, Object> customerData) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String username = authentication.getName();
            
            System.out.println("CustomerController.updateCurrentCustomer: Updating customer for username: " + username);
            
            if (username == null) {
                System.err.println("CustomerController.updateCurrentCustomer: username is null");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Username is null");
            }
            
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found: " + username));
            
            System.out.println("CustomerController.updateCurrentCustomer: Found user with ID: " + user.getId());
            
            Customer customer = customerRepo.findByUserId(user.getId()).orElse(null);
            
            // Якщо Customer не знайдено, створюємо нового
            if (customer == null) {
                System.out.println("CustomerController.updateCurrentCustomer: Customer not found, creating new one");
                // Створюємо нового Customer
                String name = (String) customerData.get("name");
                String email = (String) customerData.get("email");
                Object ageObj = customerData.get("age");
                Integer age = ageObj instanceof Integer ? (Integer) ageObj : 
                             ageObj instanceof Number ? ((Number) ageObj).intValue() : null;
                
                if (name == null || email == null || age == null) {
                    return ResponseEntity.badRequest().body("Name, email, and age are required");
                }
                
                customer = new Customer(name, email, age);
                customer.setUser(user);
                customer = customerRepo.save(customer);
                System.out.println("CustomerController.updateCurrentCustomer: Created new customer with ID: " + customer.getId());
            }
            
            // Оновлюємо поля
            if (customerData.containsKey("name")) {
                String name = (String) customerData.get("name");
                if (name != null && !name.trim().isEmpty()) {
                    customer.setName(name.trim());
                    System.out.println("CustomerController.updateCurrentCustomer: Updated name to: " + name);
                }
            }
            if (customerData.containsKey("email")) {
                String email = (String) customerData.get("email");
                if (email != null && !email.trim().isEmpty()) {
                    customer.setEmail(email.trim());
                    System.out.println("CustomerController.updateCurrentCustomer: Updated email to: " + email);
                }
            }
            if (customerData.containsKey("age")) {
                Object ageObj = customerData.get("age");
                if (ageObj != null) {
                    Integer age;
                    if (ageObj instanceof Integer) {
                        age = (Integer) ageObj;
                    } else if (ageObj instanceof Number) {
                        age = ((Number) ageObj).intValue();
                    } else {
                        age = Integer.parseInt(ageObj.toString());
                    }
                    customer.setAge(age);
                    System.out.println("CustomerController.updateCurrentCustomer: Updated age to: " + age);
                }
            }
            
            // Оновлюємо роботодавця
            if (customerData.containsKey("employerId")) {
                Object employerIdObj = customerData.get("employerId");
                if (employerIdObj != null && !employerIdObj.toString().isEmpty()) {
                    try {
                        Long employerId = employerIdObj instanceof Number ? 
                                ((Number) employerIdObj).longValue() : 
                                Long.parseLong(employerIdObj.toString());
                        
                        if (employerId > 0) {
                            Employer employer = employerRepo.findById(employerId).orElse(null);
                            if (employer != null) {
                                Set<Employer> employers = new HashSet<>();
                                employers.add(employer);
                                customer.setEmployers(employers);
                            } else {
                                System.err.println("Employer not found with ID: " + employerId);
                            }
                        }
                    } catch (NumberFormatException e) {
                        System.err.println("Invalid employerId format: " + employerIdObj);
                    }
                }
            } else if (customerData.containsKey("employerName")) {
                // Створюємо/підставляємо роботодавця за назвою
                String employerName = (String) customerData.get("employerName");
                String employerAddress = (String) customerData.get("employerAddress");
                
                if (employerName != null && !employerName.trim().isEmpty()) {
                    Employer employer = employerRepo.findByNameIgnoreCase(employerName.trim()).orElse(null);
                    if (employer == null) {
                        try {
                            employer = new Employer();
                            employer.setName(employerName.trim());
                            employer.setAddress(employerAddress != null ? employerAddress.trim() : "");
                            employer = employerRepo.save(employer);
                        } catch (org.springframework.dao.DataIntegrityViolationException dup) {
                            // Якщо паралельно створили — повторно зчитаємо
                            employer = employerRepo.findByNameIgnoreCase(employerName.trim()).orElse(null);
                        }
                    }
                    if (employer != null) {
                        Set<Employer> employers = new HashSet<>();
                        employers.add(employer);
                        customer.setEmployers(employers);
                    }
                }
            }
            
            // Переконуємося що User встановлено
            if (customer.getUser() == null) {
                customer.setUser(user);
            }
            
            Customer updatedCustomer = customerRepo.save(customer);
            System.out.println("CustomerController.updateCurrentCustomer: Customer saved successfully with ID: " + updatedCustomer.getId());
            
            // Перевіряємо що збереження пройшло успішно
            Customer savedCheck = customerRepo.findByUserId(user.getId()).orElse(null);
            if (savedCheck == null) {
                System.err.println("CustomerController.updateCurrentCustomer: WARNING! Customer not found after save!");
            }
            
            return ResponseEntity.ok(updatedCustomer);
        } catch (RuntimeException e) {
            System.err.println("CustomerController.updateCurrentCustomer: RuntimeException: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Error: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("CustomerController.updateCurrentCustomer: Exception: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Internal server error: " + e.getMessage());
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

    @GetMapping("/{customerId}/accounts")
    public ResponseEntity<List<Account>> getCustomerAccounts(@PathVariable Long customerId) {
        try {
            Customer customer = customerService.getCustomerById(customerId);
            if (customer == null) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(customer.getAccounts());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/{customerId}/accounts")
    public ResponseEntity<Account> createAccountForCustomer(
            @PathVariable Long customerId,
            @RequestBody Map<String, String> requestBody) {
        try {
            Currency currency = Currency.valueOf(requestBody.get("currency"));
            // email та password не потрібні для створення акаунту, але залишаємо для сумісності
            String email = requestBody.get("email");
            String password = requestBody.get("password");
            Account account = customerService.createAccountForCustomer(customerId, currency, email, password);
            return ResponseEntity.status(HttpStatus.CREATED).body(account);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
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