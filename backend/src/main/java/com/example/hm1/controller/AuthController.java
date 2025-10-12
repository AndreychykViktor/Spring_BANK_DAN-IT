package com.example.hm1.controller;

import com.example.hm1.dao.RoleRepository;
import com.example.hm1.dao.UserRepository;
import com.example.hm1.entity.Role;
import com.example.hm1.entity.User;
import com.example.hm1.security.JwtService;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Set;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authManager;
    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest req) {
        if (userRepository.existsByUsername(req.getUsername())) {
            return ResponseEntity.badRequest().body("Username already taken");
        }
        if (userRepository.existsByEmail(req.getEmail())) {
            return ResponseEntity.badRequest().body("Email already used");
        }
        Role userRole = roleRepository.findByName("ROLE_USER")
                .orElseGet(() -> roleRepository.save(Role.builder().name("ROLE_USER").build()));
        User user = User.builder()
                .username(req.getUsername())
                .email(req.getEmail())
                .password(passwordEncoder.encode(req.getPassword()))
                .enabled(true)
                .roles(Set.of(userRole))
                .build();
        userRepository.save(user);

        String token = jwtService.generateToken(user.getUsername(), new String[]{"ROLE_USER"});
        return ResponseEntity.ok(new AuthResponse(token));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest req) {
        Authentication auth = new UsernamePasswordAuthenticationToken(req.getUsername(), req.getPassword());
        authManager.authenticate(auth);

        User user = userRepository.findByUsername(req.getUsername()).orElseThrow();
        String[] roles = user.getRoles().stream().map(Role::getName).toArray(String[]::new);
        String token = jwtService.generateToken(user.getUsername(), roles);
        return ResponseEntity.ok(new AuthResponse(token));
    }

    @Data
    public static class RegisterRequest {
        @NotBlank
        private String username;
        @Email
        @NotBlank
        private String email;
        @NotBlank
        private String password;
    }

    @Data
    public static class LoginRequest {
        @NotBlank
        private String username;
        @NotBlank
        private String password;
    }

    @Data
    public static class AuthResponse {
        private final String accessToken;
    }
}