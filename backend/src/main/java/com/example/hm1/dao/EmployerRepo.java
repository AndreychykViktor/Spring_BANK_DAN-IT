package com.example.hm1.dao;

import com.example.hm1.entity.Employer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EmployerRepo extends JpaRepository<Employer, Long> {
}