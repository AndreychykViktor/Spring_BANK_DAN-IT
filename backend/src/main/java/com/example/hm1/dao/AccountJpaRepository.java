package com.example.hm1.dao;

import com.example.hm1.entity.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AccountJpaRepository extends JpaRepository<Account, Long> {
    Account findByNumber(String number);
    List<Account> findByCustomer_Id(Long customerId);
}
