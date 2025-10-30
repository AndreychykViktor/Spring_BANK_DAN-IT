package com.example.hm1.dao;

import com.example.hm1.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CustomerJpaRepository extends JpaRepository<Customer, Long> {
    Optional<Customer> findByUser_Id(Long userId);
    Customer findByEmail(String email);
}
