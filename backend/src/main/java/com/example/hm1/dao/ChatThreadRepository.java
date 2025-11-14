package com.example.hm1.dao;

import com.example.hm1.entity.ChatThread;
import com.example.hm1.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ChatThreadRepository extends JpaRepository<ChatThread, Long> {

    @Query("SELECT t FROM ChatThread t JOIN t.participants p WHERE p = :user")
    List<ChatThread> findAllByParticipant(@Param("user") User user);

    @Query("""
            SELECT t FROM ChatThread t
            JOIN t.participants p1
            JOIN t.participants p2
            WHERE p1 = :user1 AND p2 = :user2
            """)
    Optional<ChatThread> findThreadBetween(@Param("user1") User user1, @Param("user2") User user2);
}

