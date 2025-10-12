package com.example.hm1.dao;

import com.example.hm1.entity.Customer;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

@Repository
public class CustomerRepoImpl implements CustomerRepo {
    private final List<Customer> customers = new ArrayList<>();
    private final AtomicLong idGenerator = new AtomicLong(1);

    @Override
    public Customer save(Customer customer) {
        if (customer.getId() == null) {
            customer.setId(idGenerator.getAndIncrement());
        }

        Customer existing = getOne(customer.getId());
        if (existing != null) {
            customers.remove(existing);
        }

        customers.add(customer);
        return customer;
    }

    @Override
    public boolean delete(Customer customer) {
        return customers.remove(customer);
    }

    @Override
    public void deleteAll(List<Customer> entities) {
        customers.removeAll(entities);
    }

    @Override
    public void saveAll(List<Customer> entities) {
        for (Customer customer : entities) {
            save(customer);
        }
    }

    @Override
    public List<Customer> findAll() {
        return new ArrayList<>(customers);
    }

    @Override
    public boolean deleteById(long id) {
        return customers.removeIf(customer -> customer.getId() != null && customer.getId() == id);
    }

    @Override
    public Customer getOne(long id) {
        return customers.stream()
                .filter(customer -> customer.getId() != null && customer.getId() == id)
                .findFirst()
                .orElse(null);
    }

    @Override
    public Customer findByEmail(String email) {
        return customers.stream()
                .filter(customer -> email.equals(customer.getEmail()))
                .findFirst()
                .orElse(null);
    }
}