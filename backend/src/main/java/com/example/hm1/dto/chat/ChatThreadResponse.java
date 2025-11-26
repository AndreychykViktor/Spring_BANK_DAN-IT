package com.example.hm1.dto.chat;

import com.example.hm1.entity.ChatThread;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

public class ChatThreadResponse {

    private Long id;
    private List<ChatContactResponse> participants;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public ChatThreadResponse() {
    }

    public ChatThreadResponse(Long id, List<ChatContactResponse> participants,
                             LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.participants = participants;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static ChatThreadResponse from(ChatThread thread) {
        List<ChatContactResponse> participants = thread.getParticipants().stream()
                .map(user -> new ChatContactResponse(user.getId(), user.getUsername(), user.getUsername() + "@example.com"))
                .collect(Collectors.toList());

        return new ChatThreadResponse(
                thread.getId(),
                participants,
                thread.getCreatedAt(),
                thread.getUpdatedAt()
        );
    }

    // Getters and setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public List<ChatContactResponse> getParticipants() {
        return participants;
    }

    public void setParticipants(List<ChatContactResponse> participants) {
        this.participants = participants;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}

