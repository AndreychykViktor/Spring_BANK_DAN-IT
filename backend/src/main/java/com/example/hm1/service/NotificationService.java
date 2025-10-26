package com.example.hm1.service;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
public class NotificationService {

    private final SimpMessagingTemplate messagingTemplate;

    public NotificationService(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public void sendAccountUpdateNotification(String accountNumber, String operation, Double amount, Double newBalance) {
        Map<String, Object> notification = new HashMap<>();
        notification.put("accountNumber", accountNumber);
        notification.put("operation", operation);
        notification.put("amount", amount);
        notification.put("newBalance", newBalance);
        notification.put("timestamp", LocalDateTime.now());
        notification.put("message", String.format("Account %s: %s of %.2f completed. New balance: %.2f", 
                accountNumber, operation, amount, newBalance));

        
        // Send to all subscribers of the account updates topic
        messagingTemplate.convertAndSend("/topic/account-updates", notification);
    }

    public void sendTransactionNotification(String accountNumber, String transactionType, Double amount, String status) {
        Map<String, Object> notification = new HashMap<>();
        notification.put("accountNumber", accountNumber);
        notification.put("transactionType", transactionType);
        notification.put("amount", amount);
        notification.put("status", status);
        notification.put("timestamp", LocalDateTime.now());
        notification.put("message", String.format("Transaction %s for account %s: %.2f - Status: %s", 
                transactionType, accountNumber, amount, status));

        
        // Send to all subscribers of the transaction updates topic
        messagingTemplate.convertAndSend("/topic/transaction-updates", notification);
    }

    public void sendSystemNotification(String message, String type) {
        Map<String, Object> notification = new HashMap<>();
        notification.put("message", message);
        notification.put("type", type);
        notification.put("timestamp", LocalDateTime.now());

        
        // Send to all subscribers of the system notifications topic
        messagingTemplate.convertAndSend("/topic/system-notifications", notification);
    }
}
