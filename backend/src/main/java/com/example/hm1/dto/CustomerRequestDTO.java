package com.example.hm1.dto;

import jakarta.validation.constraints.*;
import java.util.List;

public class CustomerRequestDTO {
    
    @NotBlank(message = "Ім'я обов'язкове")
    @Size(min = 2, message = "Ім'я має містити щонайменше 2 символи")
    private String name;
    
    @NotBlank(message = "Email обов'язковий")
    @Email(message = "Email має бути валідним", regexp = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
    private String email;
    
    @NotNull(message = "Вік обов'язковий")
    @Min(value = 18, message = "Вік має бути не менше 18 років")
    @Max(value = 150, message = "Вік має бути не більше 150 років")
    private Integer age;
    
    @Pattern(regexp = "^\\+?[1-9]\\d{1,14}$", message = "Телефон має бути валідним (формат: +380XXXXXXXXX або 0XXXXXXXXX)")
    private String phone;
    
    private Long employerId;
    private String employerName;
    private String employerAddress;
    private List<Long> employerIds;

    public CustomerRequestDTO() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Integer getAge() {
        return age;
    }

    public void setAge(Integer age) {
        this.age = age;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public Long getEmployerId() {
        return employerId;
    }

    public void setEmployerId(Long employerId) {
        this.employerId = employerId;
    }

    public String getEmployerName() {
        return employerName;
    }

    public void setEmployerName(String employerName) {
        this.employerName = employerName;
    }

    public String getEmployerAddress() {
        return employerAddress;
    }

    public void setEmployerAddress(String employerAddress) {
        this.employerAddress = employerAddress;
    }

    public List<Long> getEmployerIds() {
        return employerIds;
    }

    public void setEmployerIds(List<Long> employerIds) {
        this.employerIds = employerIds;
    }
}

