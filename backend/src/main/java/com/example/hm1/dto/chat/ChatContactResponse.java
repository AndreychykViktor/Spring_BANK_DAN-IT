package com.example.hm1.dto.chat;

public class ChatContactResponse {

    private Long id;
    private String username;

    public ChatContactResponse(Long id, String username) {
        this.id = id;
        this.username = username;
    }

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }
}

