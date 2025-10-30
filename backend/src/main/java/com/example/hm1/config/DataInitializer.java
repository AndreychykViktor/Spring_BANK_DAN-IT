package com.example.hm1.config;

import com.example.hm1.dao.UserRepository;
import com.example.hm1.dao.EmployerRepo;
import com.example.hm1.entity.User;
import com.example.hm1.entity.Employer;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepo;
    private final EmployerRepo employerRepo;

    public DataInitializer(UserRepository userRepo, EmployerRepo employerRepo) {
        this.userRepo = userRepo;
        this.employerRepo = employerRepo;
    }

    @Override
    public void run(String... args) {
        System.out.println("DataInitializer: Loading data from database into memory...");
        
        try {
            // Завантажуємо всіх користувачів з БД
            List<User> users = userRepo.findAll();
            System.out.println("DataInitializer: Found " + users.size() + " users in database");
            
            // Завантажуємо всіх клієнтів з БД (якщо використовується JPA, то через Query)
            // Але оскільки CustomerRepo in-memory, потрібно завантажити Customer з БД через SQL або створити JPA репозиторій
            // Поки що залишаємо як є, але додамо логування
            
            // Завантажуємо всіх роботодавців з БД в пам'ять
            List<Employer> employers = employerRepo.findAll();
            System.out.println("DataInitializer: Found " + employers.size() + " employers in database");
            
            System.out.println("DataInitializer: Data loading completed");
        } catch (Exception e) {
            System.err.println("DataInitializer: Error loading data: " + e.getMessage());
            e.printStackTrace();
        }
    }
}