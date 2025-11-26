package com.example.hm1.dto.chat;

public class ChatContactResponse {

    private Long id;
    private String username;
    private String email;

    public ChatContactResponse() {
    }

    public ChatContactResponse(Long id, String username) {
        this.id = id;
        this.username = username;
        this.email = null;
    }

    public ChatContactResponse(Long id, String username, String email) {
        this.id = id;
        this.username = username;
        this.email = email;
    }

    // Getters and setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}

