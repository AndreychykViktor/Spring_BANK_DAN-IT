package com.example.hm1.dto.chat;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class ChatMessageRequest {

    private Long threadId;

    @NotNull(message = "Recipient user ID is required")
    private Long recipientUserId;

    @NotBlank(message = "Message content is required")
    private String content;

    private Long senderUserId; // Will be set by the controller from authentication

    public ChatMessageRequest() {
    }

    public ChatMessageRequest(Long threadId, Long recipientUserId, String content) {
        this.threadId = threadId;
        this.recipientUserId = recipientUserId;
        this.content = content;
    }

    public Long getThreadId() {
        return threadId;
    }

    public void setThreadId(Long threadId) {
        this.threadId = threadId;
    }

    public Long getRecipientUserId() {
        return recipientUserId;
    }

    public void setRecipientUserId(Long recipientUserId) {
        this.recipientUserId = recipientUserId;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Long getSenderUserId() {
        return senderUserId;
    }

    public void setSenderUserId(Long senderUserId) {
        this.senderUserId = senderUserId;
    }
}

