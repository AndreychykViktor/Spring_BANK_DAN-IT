package com.example.hm1.config;

import com.example.hm1.dao.RoleRepository;
import com.example.hm1.dao.UserRepository;
import com.example.hm1.entity.Role;
import com.example.hm1.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Set;

@Configuration
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final RoleRepository roleRepo;
    private final UserRepository userRepo;
    private final PasswordEncoder encoder;

    @Override
    public void run(String... args) {
        Role roleUser = roleRepo.findByName("ROLE_USER").orElseGet(() -> roleRepo.save(Role.builder().name("ROLE_USER").build()));
        Role roleAdmin = roleRepo.findByName("ROLE_ADMIN").orElseGet(() -> roleRepo.save(Role.builder().name("ROLE_ADMIN").build()));

        if (userRepo.findByUsername("admin").isEmpty()) {
            User admin = User.builder()
                    .username("admin")
                    .email("admin@example.com")
                    .password(encoder.encode("admin123"))
                    .enabled(true)
                    .roles(Set.of(roleUser, roleAdmin))
                    .build();
            userRepo.save(admin);
        }
    }
}