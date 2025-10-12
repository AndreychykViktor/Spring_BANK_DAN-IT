package com.example.hm1.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.HashSet;
import java.util.Set;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "employers")
public class Employer extends AbstractEntity {
    private String name;
    private String address;

    @ManyToMany(mappedBy = "employers", cascade = CascadeType.ALL)
    private Set<Customer> customers = new HashSet<>();
}