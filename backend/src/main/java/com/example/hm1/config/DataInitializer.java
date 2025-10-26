package com.example.hm1.config;

import com.example.hm1.dao.RoleRepository;
import com.example.hm1.dao.UserRepository;
import com.example.hm1.dao.CustomerRepo;
import com.example.hm1.entity.Role;
import com.example.hm1.entity.User;
import com.example.hm1.entity.Customer;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Set;

@Configuration
public class DataInitializer implements CommandLineRunner {

    private final RoleRepository roleRepo;
    private final UserRepository userRepo;
    private final CustomerRepo customerRepo;
    private final PasswordEncoder encoder;

    public DataInitializer(RoleRepository roleRepo, UserRepository userRepo, CustomerRepo customerRepo, PasswordEncoder encoder) {
        this.roleRepo = roleRepo;
        this.userRepo = userRepo;
        this.customerRepo = customerRepo;
        this.encoder = encoder;
    }

    @Override
    public void run(String... args) {
        // Skip initialization for now
        System.out.println("DataInitializer: Skipping initialization");
    }
}