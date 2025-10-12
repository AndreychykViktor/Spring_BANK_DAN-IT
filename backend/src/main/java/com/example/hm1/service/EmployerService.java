package com.example.hm1.service;

import com.example.hm1.dao.EmployerRepo;
import com.example.hm1.entity.Employer;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EmployerService {

    private final EmployerRepo employerRepo;

    public EmployerService(EmployerRepo employerRepo) {
        this.employerRepo = employerRepo;
    }

    public Employer save(Employer employer) {
        return employerRepo.save(employer);
    }

    public List<Employer> findAll() {
        return employerRepo.findAll();
    }

    public Employer findById(Long id) {
        return employerRepo.findById(id).orElse(null);
    }

    public void deleteById(Long id) {
        employerRepo.deleteById(id);
    }
}