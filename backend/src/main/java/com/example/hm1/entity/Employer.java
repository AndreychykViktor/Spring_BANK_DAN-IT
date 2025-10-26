package com.example.hm1.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "employers")
public class Employer extends AbstractEntity {
    private String name;
    private String address;

    @ManyToMany(mappedBy = "employers", cascade = CascadeType.ALL)
    private Set<Customer> customers = new HashSet<>();

    public Employer() {
    }

    public String getName() {
        return this.name;
    }

    public String getAddress() {
        return this.address;
    }

    public Set<Customer> getCustomers() {
        return this.customers;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public void setCustomers(Set<Customer> customers) {
        this.customers = customers;
    }

    public String toString() {
        return "Employer(name=" + this.getName() + ", address=" + this.getAddress() + ", customers=" + this.getCustomers() + ")";
    }

    public boolean equals(final Object o) {
        if (o == this) return true;
        if (!(o instanceof Employer)) return false;
        final Employer other = (Employer) o;
        if (!other.canEqual((Object) this)) return false;
        if (!super.equals(o)) return false;
        final Object this$name = this.getName();
        final Object other$name = other.getName();
        if (this$name == null ? other$name != null : !this$name.equals(other$name)) return false;
        final Object this$address = this.getAddress();
        final Object other$address = other.getAddress();
        if (this$address == null ? other$address != null : !this$address.equals(other$address)) return false;
        final Object this$customers = this.getCustomers();
        final Object other$customers = other.getCustomers();
        if (this$customers == null ? other$customers != null : !this$customers.equals(other$customers)) return false;
        return true;
    }

    protected boolean canEqual(final Object other) {
        return other instanceof Employer;
    }

    public int hashCode() {
        final int PRIME = 59;
        int result = super.hashCode();
        final Object $name = this.getName();
        result = result * PRIME + ($name == null ? 43 : $name.hashCode());
        final Object $address = this.getAddress();
        result = result * PRIME + ($address == null ? 43 : $address.hashCode());
        final Object $customers = this.getCustomers();
        result = result * PRIME + ($customers == null ? 43 : $customers.hashCode());
        return result;
    }
}