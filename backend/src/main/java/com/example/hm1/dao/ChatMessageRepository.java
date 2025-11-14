package com.example.hm1.dao;

import com.example.hm1.entity.ChatMessage;
import com.example.hm1.entity.ChatThread;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    List<ChatMessage> findByThreadOrderByCreatedAtAsc(ChatThread thread);

    List<ChatMessage> findByThreadOrderByCreatedAtDesc(ChatThread thread, Pageable pageable);
}

