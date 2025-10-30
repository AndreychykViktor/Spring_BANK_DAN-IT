package com.example.hm1.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Entity
@Table(name = "customers")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Customer extends AbstractEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false)
    private Integer age;

    @Column(unique = true, nullable = false, length = 150)
    private String email;

    @JsonBackReference
    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @JsonManagedReference
    @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Account> accounts = new ArrayList<>();

    public Customer(String name, String email, Integer age) {
        this.name = name;
        this.email = email;
        this.age = age;
        this.accounts = new ArrayList<>();
    }

    public Customer(Long id, String name, String email, Integer age, List<Account> accounts, Set<Employer> employers, User user) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.age = age;
        this.accounts = accounts;
        this.employers = employers;
        this.user = user;
    }

    public Customer() {
    }

    public static CustomerBuilder builder() {
        return new CustomerBuilder();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Customer customer = (Customer) o;
        return Objects.equals(id, customer.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Customer{" +
               "id=" + id +
               ", name='" + name + '\'' +
               ", age=" + age +
               ", accountsCount=" + (accounts != null ? accounts.size() : 0) +
               '}';
    }

    public String getName() {
        return name;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getId() {
        return id;
    }


    public void setAccounts(List<Account> accounts) {
        this.accounts = accounts;
    }

    @ManyToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinTable(
            name = "customer_employer",
            joinColumns = @JoinColumn(name = "customer_id"),
            inverseJoinColumns = @JoinColumn(name = "employer_id")
    )
    private Set<Employer> employers = new HashSet<>();




    public Integer getAge() {
        return this.age;
    }

    public List<Account> getAccounts() {
        return this.accounts;
    }

    public Set<Employer> getEmployers() {
        return this.employers;
    }


    public void setName(String name) {
        this.name = name;
    }


    public void setAge(Integer age) {
        this.age = age;
    }

    public void setEmployers(Set<Employer> employers) {
        this.employers = employers;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
        if (user != null && user.getCustomer() != this) {
            user.setCustomer(this);
        }
    }



    public static class CustomerBuilder {
        private Long id;
        private String name;
        private String email;
        private Integer age;
        private List<Account> accounts;
        private Set<Employer> employers;
        private User user;

        CustomerBuilder() {
        }

        public CustomerBuilder id(Long id) {
            this.id = id;
            return this;
        }

        public CustomerBuilder name(String name) {
            this.name = name;
            return this;
        }

        public CustomerBuilder email(String email) {
            this.email = email;
            return this;
        }

        public CustomerBuilder age(Integer age) {
            this.age = age;
            return this;
        }

        public CustomerBuilder accounts(List<Account> accounts) {
            this.accounts = accounts;
            return this;
        }

        public CustomerBuilder employers(Set<Employer> employers) {
            this.employers = employers;
            return this;
        }

        public CustomerBuilder user(User user) {
            this.user = user;
            return this;
        }

        public Customer build() {
            return new Customer(this.id, this.name, this.email, this.age, this.accounts, this.employers, this.user);
        }

        public String toString() {
            return "Customer.CustomerBuilder(id=" + this.id + ", name=" + this.name + ", email=" + this.email + ", age=" + this.age + ", accounts=" + this.accounts + ", employers=" + this.employers + ")";
        }
    }
}