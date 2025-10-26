package com.example.hm1.controller;

import com.example.hm1.service.AccountService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AccountController.class)
class AccountControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AccountService accountService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void deposit_ShouldReturnSuccess_WhenValidAmount() throws Exception {
        // Given
        Map<String, Double> request = Map.of("amount", 100.0);
        when(accountService.deposit("123456", 100.0)).thenReturn(true);

        // When & Then
        mockMvc.perform(post("/api/accounts/123456/deposit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().string("Deposit successful"));

        verify(accountService).deposit("123456", 100.0);
    }

    @Test
    void deposit_ShouldReturnBadRequest_WhenInvalidAmount() throws Exception {
        // Given
        Map<String, Double> request = Map.of("amount", -50.0);
        when(accountService.deposit("123456", -50.0)).thenReturn(false);

        // When & Then
        mockMvc.perform(post("/api/accounts/123456/deposit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Deposit failed"));

        verify(accountService).deposit("123456", -50.0);
    }

    @Test
    void deposit_ShouldReturnBadRequest_WhenAmountIsNull() throws Exception {
        // Given
        Map<String, Double> request = Map.of("invalid", 100.0);

        // When & Then
        mockMvc.perform(post("/api/accounts/123456/deposit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Amount is required"));

        verify(accountService, never()).deposit(anyString(), anyDouble());
    }

    @Test
    void withdraw_ShouldReturnSuccess_WhenValidAmount() throws Exception {
        // Given
        Map<String, Double> request = Map.of("amount", 50.0);
        when(accountService.withdraw("123456", 50.0)).thenReturn(true);

        // When & Then
        mockMvc.perform(post("/api/accounts/123456/withdraw")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().string("Withdrawal successful"));

        verify(accountService).withdraw("123456", 50.0);
    }

    @Test
    void withdraw_ShouldReturnBadRequest_WhenInsufficientFunds() throws Exception {
        // Given
        Map<String, Double> request = Map.of("amount", 1000.0);
        when(accountService.withdraw("123456", 1000.0)).thenReturn(false);

        // When & Then
        mockMvc.perform(post("/api/accounts/123456/withdraw")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Insufficient funds or account not found"));

        verify(accountService).withdraw("123456", 1000.0);
    }

    @Test
    void withdraw_ShouldReturnBadRequest_WhenAmountIsNull() throws Exception {
        // Given
        Map<String, Double> request = Map.of("invalid", 50.0);

        // When & Then
        mockMvc.perform(post("/api/accounts/123456/withdraw")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Amount is required"));

        verify(accountService, never()).withdraw(anyString(), anyDouble());
    }
}
