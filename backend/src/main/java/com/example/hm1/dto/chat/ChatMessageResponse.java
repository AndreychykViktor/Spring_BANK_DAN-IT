package com.example.hm1.dto.chat;

import com.example.hm1.entity.ChatMessage;

import java.time.LocalDateTime;

public class ChatMessageResponse {

    private Long id;
    private Long threadId;
    private Long senderId;
    private String senderUsername;
    private String content;
    private LocalDateTime createdAt;
    private String messageType;
    private TransferPayload transfer;

    public ChatMessageResponse() {
    }

    public ChatMessageResponse(Long id, Long threadId, Long senderId, String senderUsername,
                              String content, LocalDateTime createdAt, String messageType) {
        this.id = id;
        this.threadId = threadId;
        this.senderId = senderId;
        this.senderUsername = senderUsername;
        this.content = content;
        this.createdAt = createdAt;
        this.messageType = messageType;
    }

    public static ChatMessageResponse from(ChatMessage message) {
        ChatMessageResponse response = new ChatMessageResponse(
                message.getId(),
                message.getThread().getId(),
                message.getSender().getId(),
                message.getSender().getUsername(),
                message.getContent(),
                message.getCreatedAt(),
                message.getMessageType() != null ? message.getMessageType().name() : "TEXT"
        );

        if (message.getMessageType() == ChatMessage.MessageType.TRANSFER) {
            TransferPayload transfer = new TransferPayload();
            transfer.setAmount(message.getTransferAmount() != null ? message.getTransferAmount() : java.math.BigDecimal.ZERO);
            transfer.setCurrency(message.getTransferCurrency());
            transfer.setStatus(message.getTransferStatus() != null ? message.getTransferStatus().name() : "PENDING");
            response.setTransfer(transfer);
        }

        return response;
    }

    // Getters and setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getThreadId() {
        return threadId;
    }

    public void setThreadId(Long threadId) {
        this.threadId = threadId;
    }

    public Long getSenderId() {
        return senderId;
    }

    public void setSenderId(Long senderId) {
        this.senderId = senderId;
    }

    public String getSenderUsername() {
        return senderUsername;
    }

    public void setSenderUsername(String senderUsername) {
        this.senderUsername = senderUsername;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getMessageType() {
        return messageType;
    }

    public void setMessageType(String messageType) {
        this.messageType = messageType;
    }

    public TransferPayload getTransfer() {
        return transfer;
    }

    public void setTransfer(TransferPayload transfer) {
        this.transfer = transfer;
    }

    public static class TransferPayload {
        private java.math.BigDecimal amount;
        private String currency;
        private String status;

        public java.math.BigDecimal getAmount() {
            return amount;
        }

        public void setAmount(java.math.BigDecimal amount) {
            this.amount = amount;
        }

        public String getCurrency() {
            return currency;
        }

        public void setCurrency(String currency) {
            this.currency = currency;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }
    }
}

