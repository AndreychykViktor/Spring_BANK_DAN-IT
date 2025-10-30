package com.example.hm1.controller;

import com.example.hm1.dao.UserRepository;
import com.example.hm1.dao.RoleRepository;
import com.example.hm1.dao.CustomerRepo;
import com.example.hm1.entity.Role;
import com.example.hm1.entity.User;
import com.example.hm1.entity.Customer;
import com.example.hm1.service.TransactionService;
import com.example.hm1.entity.Transaction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final CustomerRepo customerRepo;
    private final TransactionService transactionService;

    @Autowired
    public AdminController(UserRepository userRepository, RoleRepository roleRepository, CustomerRepo customerRepo, TransactionService transactionService) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.customerRepo = customerRepo;
        this.transactionService = transactionService;
    }

    @PostMapping("/assign-admin/{username}")
    public ResponseEntity<?> assignAdminRole(@PathVariable String username) {
        try {
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            Role adminRole = roleRepository.findByName("ROLE_ADMIN")
                    .orElseThrow(() -> new RuntimeException("ROLE_ADMIN not found"));

            user.getRoles().add(adminRole);
            userRepository.save(user);

            return ResponseEntity.ok("Admin role assigned successfully to " + username);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed to assign admin role: " + e.getMessage());
        }
    }

    @PostMapping("/remove-admin/{username}")
    public ResponseEntity<?> removeAdminRole(@PathVariable String username) {
        try {
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            Role adminRole = roleRepository.findByName("ROLE_ADMIN")
                    .orElseThrow(() -> new RuntimeException("ROLE_ADMIN not found"));

            user.getRoles().remove(adminRole);
            userRepository.save(user);

            return ResponseEntity.ok("Admin role removed successfully from " + username);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed to remove admin role: " + e.getMessage());
        }
    }

    // DTO для чистої JSON-відповіді
    public static class AdminUserDto {
        public Long id;
        public String username;
        public boolean enabled;
        public List<String> roles;
        public Long customerId;
        public String customerName;
        public String customerEmail;
    }

    public static class AdminTransactionDto {
        public Long id;
        public String type;
        public String amount;
        public String description;
        public Long customerId;
        public String customerName;
        public String timestamp;
        public String status;
        public String fromAccount;
        public String toAccount;
    }

    @GetMapping("/users")
    public ResponseEntity<?> getAllUsers() {
        try {
            List<User> users = userRepository.findAll(Sort.by(Sort.Direction.ASC, "id"));
            // Підтягуємо Customer якщо існує і мапимо у DTO
            List<AdminUserDto> dto = users.stream().map(u -> {
                Customer customer = (u.getId() != null) ? customerRepo.findByUserId(u.getId()).orElse(null) : null;
                AdminUserDto d = new AdminUserDto();
                d.id = u.getId();
                d.username = u.getUsername();
                d.enabled = u.isEnabled();
                d.roles = (u.getRoles() != null) ? u.getRoles().stream().map(Role::getName).collect(Collectors.toList()) : List.of();
                d.customerId = (customer != null) ? customer.getId() : null;
                d.customerName = (customer != null) ? customer.getName() : null;
                d.customerEmail = (customer != null) ? customer.getEmail() : null;
                return d;
            }).collect(Collectors.toList());
            return ResponseEntity.ok(dto);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body("Failed to get users: " + e.getMessage());
        }
    }

    @PostMapping("/assign-role/{userId}/{roleName}")
    public ResponseEntity<?> assignRole(@PathVariable Long userId, @PathVariable String roleName) {
        try {
            User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));
            Role role = roleRepository.findByName(roleName).orElseThrow(() -> new RuntimeException("Role not found: " + roleName));
            user.getRoles().add(role);
            userRepository.save(user);
            return ResponseEntity.ok("Role assigned successfully");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed to assign role: " + e.getMessage());
        }
    }

    @DeleteMapping("/remove-role/{userId}/{roleName}")
    public ResponseEntity<?> removeRole(@PathVariable Long userId, @PathVariable String roleName) {
        try {
            User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));
            Role role = roleRepository.findByName(roleName).orElseThrow(() -> new RuntimeException("Role not found: " + roleName));
            user.getRoles().remove(role);
            userRepository.save(user);
            return ResponseEntity.ok("Role removed successfully");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed to remove role: " + e.getMessage());
        }
    }

    @GetMapping("/users/count")
    public ResponseEntity<?> getUsersCount() {
        try {
            long count = userRepository.count();
            return ResponseEntity.ok(java.util.Map.of("count", count));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body("Failed to get users count: " + e.getMessage());
        }
    }

    @GetMapping("/transactions")
    public ResponseEntity<?> getAllTransactionsDto() {
        try {
            List<Transaction> list = transactionService.getAllTransactions();
            List<AdminTransactionDto> dto = list.stream().map(t -> {
                AdminTransactionDto d = new AdminTransactionDto();
                d.id = t.getId();
                d.type = t.getType() != null ? t.getType().name() : null;
                d.amount = t.getAmount() != null ? t.getAmount().toPlainString() : null;
                d.description = t.getDescription();
                d.customerId = (t.getCustomer() != null ? t.getCustomer().getId() : null);
                d.customerName = (t.getCustomer() != null ? t.getCustomer().getName() : null);
                d.timestamp = t.getTimestamp() != null ? t.getTimestamp().toString() : null;
                d.status = t.getStatus() != null ? t.getStatus().name() : null;
                d.fromAccount = (t.getFromAccount() != null ? t.getFromAccount().getNumber() : null);
                d.toAccount = (t.getToAccount() != null ? t.getToAccount().getNumber() : null);
                return d;
            }).collect(java.util.stream.Collectors.toList());
            return ResponseEntity.ok(dto);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body("Failed to get transactions: " + e.getMessage());
        }
    }
}
