package com.example.hm1.controller;

import com.example.hm1.dao.CustomerRepo;
import com.example.hm1.dao.UserRepository;
import com.example.hm1.dao.EmployerRepo;
import com.example.hm1.dto.AccountResponseDTO;
import com.example.hm1.dto.CustomerRequestDTO;
import com.example.hm1.entity.User;
import com.example.hm1.entity.Employer;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
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

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/customers")
@CrossOrigin(origins = "*")
@Tag(name = "Customers", description = "API для управління клієнтами банку")
public class CustomerController {

    public static class CustomerDTO {
        public Long id;
        public String name;
        public String email;
        public Integer age;
        public List<String> employers;
        
        public CustomerDTO(Customer customer) {
            this.id = customer.getId();
            this.name = customer.getName();
            this.email = customer.getEmail();
            this.age = customer.getAge();
            this.employers = customer.getEmployers() != null 
                ? customer.getEmployers().stream()
                    .map(Employer::getName)
                    .collect(Collectors.toList())
                : new java.util.ArrayList<>();
        }
    }

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
    @Operation(summary = "Отримати всіх клієнтів", description = "Повертає список всіх клієнтів банку")
    @ApiResponse(responseCode = "200", description = "Успішно отримано список клієнтів")
    public ResponseEntity<List<Customer>> getAllCustomers() {
        List<Customer> customers = customerService.getAllCustomers();
        return ResponseEntity.ok(customers);
    }

    @GetMapping("/me")
    @Operation(summary = "Отримати поточного клієнта", description = "Повертає інформацію про поточного авторизованого клієнта")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Успішно отримано інформацію про клієнта"),
            @ApiResponse(responseCode = "401", description = "Користувач не авторизований"),
            @ApiResponse(responseCode = "404", description = "Клієнт не знайдений для поточного користувача")
    })
    public ResponseEntity<?> getCurrentCustomer() {
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
                             ", name: " + customer.getName() + ", email: " + customer.getEmail());
            
            if (customer.getId() == null) {
                System.err.println("CustomerController: WARNING! Customer ID is null before returning");
            }
            
            try {
                CustomerDTO dto = new CustomerDTO(customer);
                System.out.println("CustomerController: Successfully created DTO with ID: " + dto.id);
                return ResponseEntity.ok(dto);
            } catch (Exception e) {
                System.err.println("CustomerController: Error creating DTO: " + e.getMessage());
                e.printStackTrace();
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to serialize customer data"));
            }
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
    @Operation(summary = "Отримати клієнта за ID", description = "Повертає інформацію про клієнта за його унікальним ідентифікатором")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Клієнт знайдено"),
            @ApiResponse(responseCode = "404", description = "Клієнт з вказаним ID не знайдено")
    })
    public ResponseEntity<Customer> getCustomerById(
            @Parameter(description = "ID клієнта", required = true)
            @PathVariable Long id) {
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
    @Operation(summary = "Оновити дані поточного клієнта", description = "Оновлює інформацію про поточного авторизованого клієнта (ім'я, email, вік, роботодавець)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Дані клієнта успішно оновлено"),
            @ApiResponse(responseCode = "400", description = "Невалідні дані для оновлення"),
            @ApiResponse(responseCode = "401", description = "Користувач не авторизований")
    })
    public ResponseEntity<?> updateCurrentCustomer(
            @Parameter(description = "Оновлені дані клієнта", required = true)
            @Valid @RequestBody CustomerRequestDTO customerDTO) {
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
                customer = new Customer(customerDTO.getName(), customerDTO.getEmail(), customerDTO.getAge());
                customer.setUser(user);
                customer = customerRepo.save(customer);
                System.out.println("CustomerController.updateCurrentCustomer: Created new customer with ID: " + customer.getId());
            }
            
            // Оновлюємо поля
            if (customerDTO.getName() != null && !customerDTO.getName().trim().isEmpty()) {
                customer.setName(customerDTO.getName().trim());
                System.out.println("CustomerController.updateCurrentCustomer: Updated name to: " + customerDTO.getName());
            }
            if (customerDTO.getEmail() != null && !customerDTO.getEmail().trim().isEmpty()) {
                customer.setEmail(customerDTO.getEmail().trim());
                System.out.println("CustomerController.updateCurrentCustomer: Updated email to: " + customerDTO.getEmail());
            }
            if (customerDTO.getAge() != null) {
                customer.setAge(customerDTO.getAge());
                System.out.println("CustomerController.updateCurrentCustomer: Updated age to: " + customerDTO.getAge());
            }
            
            // Оновлюємо роботодавця
            if (customerDTO.getEmployerId() != null && customerDTO.getEmployerId() > 0) {
                Employer employer = employerRepo.findById(customerDTO.getEmployerId()).orElse(null);
                if (employer != null) {
                    Set<Employer> employers = new HashSet<>();
                    employers.add(employer);
                    customer.setEmployers(employers);
                } else {
                    System.err.println("Employer not found with ID: " + customerDTO.getEmployerId());
                }
            } else if (customerDTO.getEmployerName() != null && !customerDTO.getEmployerName().trim().isEmpty()) {
                String employerName = customerDTO.getEmployerName().trim();
                String employerAddress = customerDTO.getEmployerAddress() != null ? customerDTO.getEmployerAddress().trim() : "";
                
                Employer employer = employerRepo.findByNameIgnoreCase(employerName).orElse(null);
                if (employer == null) {
                    try {
                        employer = new Employer();
                        employer.setName(employerName);
                        employer.setAddress(employerAddress);
                        employer = employerRepo.save(employer);
                        System.out.println("CustomerController.updateCurrentCustomer: Created new employer: " + employer.getName() + " with ID: " + employer.getId());
                    } catch (org.springframework.dao.DataIntegrityViolationException dup) {
                        // Якщо паралельно створили — повторно зчитаємо
                        employer = employerRepo.findByNameIgnoreCase(employerName).orElse(null);
                        System.out.println("CustomerController.updateCurrentCustomer: Found existing employer after collision: " + (employer != null ? employer.getName() : "null"));
                    }
                } else {
                    System.out.println("CustomerController.updateCurrentCustomer: Using existing employer: " + employer.getName() + " with ID: " + employer.getId());
                }
                if (employer != null) {
                    // Отримуємо існуючий set або створюємо новий
                    Set<Employer> employers = customer.getEmployers();
                    if (employers == null) {
                        employers = new HashSet<>();
                    } else {
                        // Очищаємо старі зв'язки, щоб уникнути дублікатів
                        employers.clear();
                    }
                    employers.add(employer);
                    customer.setEmployers(employers);
                    System.out.println("CustomerController.updateCurrentCustomer: Set employer for customer. Employer ID: " + employer.getId());
                }
            }
            
            // Переконуємося що User встановлено
            if (customer.getUser() == null) {
                customer.setUser(user);
            }
            
            Customer updatedCustomer = customerRepo.save(customer);
            System.out.println("CustomerController.updateCurrentCustomer: Customer saved successfully with ID: " + updatedCustomer.getId());
            System.out.println("CustomerController.updateCurrentCustomer: Employers count after save: " + 
                (updatedCustomer.getEmployers() != null ? updatedCustomer.getEmployers().size() : 0));
            
            // Перевіряємо що збереження пройшло успішно
            Customer savedCheck = customerRepo.findByUserId(user.getId()).orElse(null);
            if (savedCheck == null) {
                System.err.println("CustomerController.updateCurrentCustomer: WARNING! Customer not found after save!");
            } else {
                System.out.println("CustomerController.updateCurrentCustomer: Employers count in DB check: " + 
                    (savedCheck.getEmployers() != null ? savedCheck.getEmployers().size() : 0));
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
    @Operation(summary = "Отримати рахунки клієнта", description = "Повертає список всіх рахунків вказаного клієнта, відсортовані за ID (нові спочатку)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Успішно отримано список рахунків"),
            @ApiResponse(responseCode = "404", description = "Клієнт не знайдено"),
            @ApiResponse(responseCode = "500", description = "Внутрішня помилка сервера")
    })
    public ResponseEntity<List<AccountResponseDTO>> getCustomerAccounts(
            @Parameter(description = "ID клієнта", required = true)
            @PathVariable Long customerId) {
        try {
            Customer customer = customerService.getCustomerById(customerId);
            if (customer == null) {
                return ResponseEntity.notFound().build();
            }
            Comparator<Account> byIdDesc = Comparator.comparing(Account::getId, Comparator.nullsLast(Long::compareTo)).reversed();
            List<AccountResponseDTO> accounts = customer.getAccounts() == null
                    ? List.of()
                    : customer.getAccounts().stream()
                        .sorted(byIdDesc)
                        .map(AccountResponseDTO::from)
                        .collect(Collectors.toList());
            return ResponseEntity.ok(accounts);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/{customerId}/accounts")
    @Operation(summary = "Створити рахунок для клієнта", description = "Створює новий банківський рахунок для вказаного клієнта з вказаною валютою")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Рахунок успішно створено"),
            @ApiResponse(responseCode = "400", description = "Невалідні дані (невірна валюта, відсутня валюта)"),
            @ApiResponse(responseCode = "404", description = "Клієнт не знайдено"),
            @ApiResponse(responseCode = "500", description = "Внутрішня помилка сервера")
    })
    public ResponseEntity<AccountResponseDTO> createAccountForCustomer(
            @Parameter(description = "ID клієнта", required = true)
            @PathVariable Long customerId,
            @Parameter(description = "Дані рахунку: currency (обов'язково), email, password (опціонально)", required = true)
            @RequestBody Map<String, String> requestBody) {
        try {
            System.out.println("CustomerController.createAccountForCustomer: Request received for customerId=" + customerId);
            System.out.println("CustomerController.createAccountForCustomer: Request body: " + requestBody);
            
            String currencyStr = requestBody.get("currency");
            if (currencyStr == null || currencyStr.trim().isEmpty()) {
                System.err.println("CustomerController.createAccountForCustomer: Currency is null or empty");
                return ResponseEntity.badRequest().body(null);
            }
            
            Currency currency = Currency.valueOf(currencyStr.toUpperCase());
            System.out.println("CustomerController.createAccountForCustomer: Parsed currency: " + currency);
            
            String email = requestBody.get("email");
            String password = requestBody.get("password");
            Account account = customerService.createAccountForCustomer(customerId, currency, email, password);
            System.out.println("CustomerController.createAccountForCustomer: Account created successfully");
            return ResponseEntity.status(HttpStatus.CREATED).body(AccountResponseDTO.from(account));
        } catch (IllegalArgumentException e) {
            System.err.println("CustomerController.createAccountForCustomer: IllegalArgumentException: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest().body(null);
        } catch (Exception e) {
            System.err.println("CustomerController.createAccountForCustomer: Exception: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
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