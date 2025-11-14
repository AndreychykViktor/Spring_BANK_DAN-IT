package com.example.hm1.dto.chat;

import com.example.hm1.entity.ChatThread;
import com.example.hm1.entity.User;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.stream.Collectors;

public class ChatThreadResponse {

    private Long id;
    private Set<Participant> participants;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static ChatThreadResponse from(ChatThread thread) {
        ChatThreadResponse response = new ChatThreadResponse();
        response.id = thread.getId();
        response.participants = thread.getParticipants()
                .stream()
                .map(Participant::from)
                .collect(Collectors.toSet());
        response.createdAt = thread.getCreatedAt();
        response.updatedAt = thread.getUpdatedAt();
        return response;
    }

    public Long getId() {
        return id;
    }

    public Set<Participant> getParticipants() {
        return participants;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public static class Participant {
        private Long id;
        private String username;

        public static Participant from(User user) {
            Participant participant = new Participant();
            participant.id = user.getId();
            participant.username = user.getUsername();
            return participant;
        }

        public Long getId() {
            return id;
        }

        public String getUsername() {
            return username;
        }
    }
}

