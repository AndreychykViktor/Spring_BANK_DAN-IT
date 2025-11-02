package com.example.hm1.dao;

import com.example.hm1.entity.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    
    @Query("SELECT t FROM Transaction t JOIN FETCH t.customer WHERE t.customer.id = :customerId ORDER BY t.timestamp DESC")
    List<Transaction> findByCustomerIdOrderByTimestampDesc(@Param("customerId") Long customerId);
    
    @Query("SELECT t FROM Transaction t WHERE t.customer.id = :customerId ORDER BY t.timestamp DESC")
    Page<Transaction> findByCustomerIdOrderByTimestampDescPage(@Param("customerId") Long customerId, Pageable pageable);
    
    List<Transaction> findByFromAccountIdOrToAccountIdOrderByTimestampDesc(Long fromAccountId, Long toAccountId);
    
    @Query("SELECT t FROM Transaction t WHERE t.customer.id = :customerId AND t.timestamp BETWEEN :startDate AND :endDate ORDER BY t.timestamp DESC")
    List<Transaction> findByCustomerIdAndDateRange(@Param("customerId") Long customerId, 
                                                   @Param("startDate") LocalDateTime startDate, 
                                                   @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT t FROM Transaction t WHERE t.timestamp BETWEEN :startDate AND :endDate ORDER BY t.timestamp DESC")
    List<Transaction> findAllByDateRange(@Param("startDate") LocalDateTime startDate, 
                                        @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT t FROM Transaction t ORDER BY t.timestamp DESC")
    Page<Transaction> findAllOrderByTimestampDesc(Pageable pageable);

    @Query("SELECT t FROM Transaction t ORDER BY t.timestamp DESC")
    List<Transaction> findAllOrderByTimestampDesc();
    
    List<Transaction> findByTypeOrderByTimestampDesc(Transaction.TransactionType type);
    
    List<Transaction> findByStatusOrderByTimestampDesc(Transaction.TransactionStatus status);
}
