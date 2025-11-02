package com.example.hm1.dao;

import com.example.hm1.entity.Account;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AccountJpaRepository extends JpaRepository<Account, Long> {
    @EntityGraph(attributePaths = "customer")
    Optional<Account> findByNumber(String number);
    List<Account> findByCustomer_Id(Long customerId);
    
    @Query("SELECT a.number FROM Account a WHERE a.number LIKE 'ACC-%' ORDER BY a.number DESC")
    List<String> findAllAccountNumbersOrderedDesc();
}
