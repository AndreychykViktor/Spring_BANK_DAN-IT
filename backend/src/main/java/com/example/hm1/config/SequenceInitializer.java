package com.example.hm1.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
public class SequenceInitializer {

    @Bean
    CommandLineRunner syncSequences(JdbcTemplate jdbcTemplate) {
        return args -> {
            try {
                System.out.println("SequenceInitializer: Synchronizing database sequences...");
                
                // Синхронізуємо всі sequences з максимальними ID в таблицях
                String[] tables = {"transactions", "accounts", "customers", "users", "employers", "roles"};
                
                for (String table : tables) {
                    String sequenceName = table + "_id_seq";
                    try {
                        // Отримуємо максимальний ID з таблиці
                        String sql = String.format(
                            "SELECT setval('%s', (SELECT COALESCE(MAX(id), 1) FROM %s), true)",
                            sequenceName, table
                        );
                        jdbcTemplate.execute(sql);
                        
                        // Логуємо результат
                        Long currentValue = jdbcTemplate.queryForObject(
                            "SELECT last_value FROM " + sequenceName, 
                            Long.class
                        );
                        System.out.println("SequenceInitializer: " + sequenceName + " set to " + currentValue);
                    } catch (Exception e) {
                        System.err.println("SequenceInitializer: Error syncing " + sequenceName + ": " + e.getMessage());
                        // Продовжуємо навіть якщо одна таблиця не синхронізувалась
                    }
                }
                
                System.out.println("SequenceInitializer: Database sequences synchronized successfully!");
            } catch (Exception e) {
                System.err.println("SequenceInitializer: Failed to synchronize sequences: " + e.getMessage());
                e.printStackTrace();
            }
        };
    }
}

