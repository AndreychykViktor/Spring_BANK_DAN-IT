package com.example.hm1.controller;

import com.example.hm1.dto.EmployerRequestDTO;
import com.example.hm1.entity.Employer;
import com.example.hm1.service.EmployerService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/employers")
@CrossOrigin(origins = "*")
public class EmployerController {

    private final EmployerService employerService;

    public EmployerController(EmployerService employerService) {
        this.employerService = employerService;
    }

    @PostMapping
    public Employer createEmployer(@Valid @RequestBody EmployerRequestDTO dto) {
        Employer employer = new Employer();
        employer.setName(dto.getName());
        employer.setAddress(dto.getAddress());
        return employerService.save(employer);
    }

    @GetMapping
    public List<Employer> getAllEmployers() {
        return employerService.findAll();
    }

    @GetMapping("/{id}")
    public Employer getEmployerById(@PathVariable Long id) {
        return employerService.findById(id);
    }

    @DeleteMapping("/{id}")
    public void deleteEmployer(@PathVariable Long id) {
        employerService.deleteById(id);
    }
}