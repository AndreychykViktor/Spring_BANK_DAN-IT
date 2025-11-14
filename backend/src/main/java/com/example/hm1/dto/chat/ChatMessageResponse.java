package com.example.hm1.dto.chat;

import com.example.hm1.entity.ChatMessage;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class ChatMessageResponse {

    private Long id;
    private Long threadId;
    private Long senderId;
    private String senderUsername;
    private String content;
    private ChatMessage.MessageType type;
    private LocalDateTime createdAt;
    private TransferPayload transfer;

    public static ChatMessageResponse from(ChatMessage message) {
        ChatMessageResponse response = new ChatMessageResponse();
        response.id = message.getId();
        response.threadId = message.getThread().getId();
        response.senderId = message.getSender().getId();
        response.senderUsername = message.getSender().getUsername();
        response.content = message.getContent();
        response.type = message.getMessageType();
        response.createdAt = message.getCreatedAt();

        if (message.getMessageType() == ChatMessage.MessageType.TRANSFER) {
            TransferPayload payload = new TransferPayload();
            payload.setAmount(message.getTransferAmount());
            payload.setCurrency(message.getTransferCurrency());
            payload.setStatus(message.getTransferStatus());
            payload.setTransactionId(message.getRelatedTransaction() != null ? message.getRelatedTransaction().getId() : null);
            response.transfer = payload;
        }

        return response;
    }

    public Long getId() {
        return id;
    }

    public Long getThreadId() {
        return threadId;
    }

    public Long getSenderId() {
        return senderId;
    }

    public String getSenderUsername() {
        return senderUsername;
    }

    public String getContent() {
        return content;
    }

    public ChatMessage.MessageType getType() {
        return type;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public TransferPayload getTransfer() {
        return transfer;
    }

    public static class TransferPayload {
        private BigDecimal amount;
        private String currency;
        private ChatMessage.TransferStatus status;
        private Long transactionId;

        public BigDecimal getAmount() {
            return amount;
        }

        public void setAmount(BigDecimal amount) {
            this.amount = amount;
        }

        public String getCurrency() {
            return currency;
        }

        public void setCurrency(String currency) {
            this.currency = currency;
        }

        public ChatMessage.TransferStatus getStatus() {
            return status;
        }

        public void setStatus(ChatMessage.TransferStatus status) {
            this.status = status;
        }

        public Long getTransactionId() {
            return transactionId;
        }

        public void setTransactionId(Long transactionId) {
            this.transactionId = transactionId;
        }
    }
}

