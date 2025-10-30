package com.example.hm1.dao;

import com.example.hm1.entity.Customer;
import com.example.hm1.entity.User;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@Primary
public class CustomerRepoJpaImpl implements CustomerRepo {

    private final CustomerJpaRepository jpa;

    public CustomerRepoJpaImpl(CustomerJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Customer save(Customer obj) {
        return jpa.save(obj);
    }

    @Override
    public boolean delete(Customer obj) {
        jpa.delete(obj);
        return true;
    }

    @Override
    public void deleteAll(List<Customer> entities) {
        jpa.deleteAll(entities);
    }

    @Override
    public void saveAll(List<Customer> entities) {
        jpa.saveAll(entities);
    }

    @Override
    public List<Customer> findAll() {
        return jpa.findAll();
    }

    @Override
    public boolean deleteById(long id) {
        if (jpa.existsById(id)) {
            jpa.deleteById(id);
            return true;
        }
        return false;
    }

    @Override
    public Customer getOne(long id) {
        return jpa.findById(id).orElse(null);
    }

    @Override
    public Customer findByEmail(String email) {
        return jpa.findByEmail(email);
    }

    @Override
    public Optional<Customer> findByUser(User user) {
        Long uid = user != null ? user.getId() : null;
        return findByUserId(uid);
    }

    @Override
    public Optional<Customer> findByUserId(Long userId) {
        if (userId == null) return Optional.empty();
        return jpa.findByUser_Id(userId);
    }
}
