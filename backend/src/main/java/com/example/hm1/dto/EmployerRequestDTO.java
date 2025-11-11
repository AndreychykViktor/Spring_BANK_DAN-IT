package com.example.hm1.dto;

import jakarta.validation.constraints.*;

public class EmployerRequestDTO {
    
    @NotBlank(message = "Назва компанії обов'язкова")
    @Size(min = 3, message = "Назва компанії має містити щонайменше 3 символи")
    private String name;
    
    @NotBlank(message = "Адреса компанії обов'язкова")
    @Size(min = 3, message = "Адреса компанії має містити щонайменше 3 символи")
    private String address;

    public EmployerRequestDTO() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }
}

