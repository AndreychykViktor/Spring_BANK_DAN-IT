package com.example.hm1.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    private NotificationService notificationService;

    @BeforeEach
    void setUp() {
        notificationService = new NotificationService(messagingTemplate);
    }

    @Test
    void sendAccountUpdateNotificationPublishesToAccountTopic() {
        notificationService.sendAccountUpdateNotification("ACC-001", "DEPOSIT", 100.0, 1200.0);

        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(mapClass());
        verify(messagingTemplate).convertAndSend(org.mockito.ArgumentMatchers.eq("/topic/account-updates"), captor.capture());

        Map<String, Object> payload = captor.getValue();
        assertThat(payload)
                .containsEntry("accountNumber", "ACC-001")
                .containsEntry("operation", "DEPOSIT")
                .containsEntry("amount", 100.0)
                .containsEntry("newBalance", 1200.0)
                .containsKey("timestamp")
                .containsEntry("message", "Account ACC-001: DEPOSIT of 100.00 completed. New balance: 1200.00");
    }

    @Test
    void sendTransactionNotificationPublishesToTransactionTopic() {
        notificationService.sendTransactionNotification("ACC-002", "TRANSFER", 75.5, "COMPLETED");

        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(mapClass());
        verify(messagingTemplate).convertAndSend(org.mockito.ArgumentMatchers.eq("/topic/transaction-updates"), captor.capture());

        Map<String, Object> payload = captor.getValue();
        assertThat(payload)
                .containsEntry("accountNumber", "ACC-002")
                .containsEntry("transactionType", "TRANSFER")
                .containsEntry("amount", 75.5)
                .containsEntry("status", "COMPLETED")
                .containsKey("timestamp")
                .containsEntry("message", "Transaction TRANSFER for account ACC-002: 75.50 - Status: COMPLETED");
    }

    @Test
    void sendSystemNotificationPublishesToSystemTopic() {
        notificationService.sendSystemNotification("Maintenance window", "INFO");

        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(mapClass());
        verify(messagingTemplate).convertAndSend(org.mockito.ArgumentMatchers.eq("/topic/system-notifications"), captor.capture());

        Map<String, Object> payload = captor.getValue();
        assertThat(payload)
                .containsEntry("message", "Maintenance window")
                .containsEntry("type", "INFO")
                .containsKey("timestamp");
    }

    @SuppressWarnings("unchecked")
    private static Class<Map<String, Object>> mapClass() {
        return (Class<Map<String, Object>>) (Class<?>) Map.class;
    }
}

