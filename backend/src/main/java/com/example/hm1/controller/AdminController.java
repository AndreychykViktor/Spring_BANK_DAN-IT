package com.example.hm1.controller;

import com.example.hm1.dao.UserRepository;
import com.example.hm1.dao.RoleRepository;
import com.example.hm1.entity.Role;
import com.example.hm1.entity.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Set;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    @Autowired
    public AdminController(UserRepository userRepository, RoleRepository roleRepository) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
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

    @GetMapping("/users")
    public ResponseEntity<?> getAllUsers() {
        try {
            return ResponseEntity.ok(userRepository.findAll());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed to get users: " + e.getMessage());
        }
    }
}
