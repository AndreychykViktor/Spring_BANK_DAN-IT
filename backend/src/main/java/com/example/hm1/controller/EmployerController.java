package com.example.hm1.controller;

import com.example.hm1.dto.EmployerRequestDTO;
import com.example.hm1.entity.Employer;
import com.example.hm1.service.EmployerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/employers")
@CrossOrigin(origins = "*")
@Tag(name = "Employers", description = "API для управління роботодавцями клієнтів")
public class EmployerController {

    private final EmployerService employerService;

    public EmployerController(EmployerService employerService) {
        this.employerService = employerService;
    }

    @PostMapping
    @Operation(summary = "Створити роботодавця", description = "Створює нового роботодавця з вказаною назвою та адресою")
    @ApiResponse(responseCode = "200", description = "Роботодавця успішно створено")
    public Employer createEmployer(
            @Parameter(description = "Дані роботодавця: name, address", required = true)
            @Valid @RequestBody EmployerRequestDTO dto) {
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