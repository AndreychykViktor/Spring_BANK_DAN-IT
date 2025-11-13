package com.example.hm1.controller;

import com.example.hm1.dao.AccountRepo;
import com.example.hm1.entity.Account;
import com.example.hm1.entity.Currency;
import com.example.hm1.service.AccountService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AccountController.class)
@AutoConfigureMockMvc(addFilters = false)
class AccountControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AccountService accountService;

    @MockBean
    private AccountRepo accountRepo;

    @Autowired
    private ObjectMapper objectMapper;

    private Account usdAccount;
    private Account eurAccount;

    @BeforeEach
    void setUp() {
        usdAccount = new Account();
        usdAccount.setId(1L);
        usdAccount.setNumber("123456");
        usdAccount.setCurrency(Currency.USD);
        usdAccount.setBalance(1000.0);

        eurAccount = new Account();
        eurAccount.setId(2L);
        eurAccount.setNumber("654321");
        eurAccount.setCurrency(Currency.EUR);
        eurAccount.setBalance(500.0);
    }

    @Test
    void deposit_ShouldReturnSuccess_WhenValidAmount() throws Exception {
        Map<String, Object> request = Map.of("amount", 100.0);
        when(accountRepo.findByNumber("123456")).thenReturn(usdAccount);
        when(accountService.deposit("123456", 100.0)).thenReturn(true);

        mockMvc.perform(post("/api/accounts/123456/deposit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().string("Deposit successful"));

        verify(accountRepo).findByNumber("123456");
        verify(accountService).deposit("123456", 100.0);
    }

    @Test
    void deposit_ShouldReturnNotFound_WhenAccountMissing() throws Exception {
        Map<String, Object> request = Map.of("amount", 100.0);
        when(accountRepo.findByNumber("123456")).thenReturn(null);

        mockMvc.perform(post("/api/accounts/123456/deposit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(content().string("Account not found"));

        verify(accountRepo).findByNumber("123456");
        verify(accountService, never()).deposit(anyString(), anyDouble());
    }

    @Test
    void deposit_ShouldReturnValidationError_WhenAmountInvalid() throws Exception {
        Map<String, Object> request = Map.of("amount", -50.0);

        mockMvc.perform(post("/api/accounts/123456/deposit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation failed"))
                .andExpect(jsonPath("$.errors.amount").value("Сума має бути більше 0"));

        verify(accountService, never()).deposit(anyString(), anyDouble());
    }

    @Test
    void deposit_ShouldReturnValidationError_WhenAmountMissing() throws Exception {
        Map<String, Object> request = Map.of("note", "missing amount");

        mockMvc.perform(post("/api/accounts/123456/deposit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation failed"))
                .andExpect(jsonPath("$.errors.amount").value("Сума обов'язкова"));

        verify(accountService, never()).deposit(any(), anyDouble());
    }

    @Test
    void withdraw_ShouldReturnSuccess_WhenValidAmount() throws Exception {
        Map<String, Object> request = Map.of("amount", 50.0);
        when(accountRepo.findByNumber("123456")).thenReturn(usdAccount);
        when(accountService.withdraw("123456", 50.0)).thenReturn(true);

        mockMvc.perform(post("/api/accounts/123456/withdraw")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().string("Withdrawal successful"));

        verify(accountRepo).findByNumber("123456");
        verify(accountService).withdraw("123456", 50.0);
    }

    @Test
    void withdraw_ShouldReturnNotFound_WhenAccountMissing() throws Exception {
        Map<String, Object> request = Map.of("amount", 50.0);
        when(accountRepo.findByNumber("123456")).thenReturn(null);

        mockMvc.perform(post("/api/accounts/123456/withdraw")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(content().string("Account not found"));

        verify(accountRepo).findByNumber("123456");
        verify(accountService, never()).withdraw(anyString(), anyDouble());
    }

    @Test
    void withdraw_ShouldReturnValidationError_WhenAmountInvalid() throws Exception {
        Map<String, Object> request = Map.of("amount", -10.0);

        mockMvc.perform(post("/api/accounts/123456/withdraw")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation failed"))
                .andExpect(jsonPath("$.errors.amount").value("Сума має бути більше 0"));

        verify(accountService, never()).withdraw(anyString(), anyDouble());
    }

    @Test
    void transfer_ShouldReturnSuccess_WhenValidData() throws Exception {
        Map<String, Object> request = Map.of(
                "toAccountNumber", "654321",
                "amount", 200.0
        );

        when(accountRepo.findByNumber("123456")).thenReturn(usdAccount);
        when(accountRepo.findByNumber("654321")).thenReturn(eurAccount);
        when(accountService.transfer("123456", "654321", 200.0)).thenReturn(true);

        mockMvc.perform(post("/api/accounts/123456/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().string("Transfer successful"));

        verify(accountRepo, times(2)).findByNumber(anyString());
        verify(accountService).transfer("123456", "654321", 200.0);
    }

    @Test
    void transfer_ShouldReturnNotFound_WhenDestinationMissing() throws Exception {
        Map<String, Object> request = Map.of(
                "toAccountNumber", "654321",
                "amount", 200.0
        );

        when(accountRepo.findByNumber("123456")).thenReturn(usdAccount);
        when(accountRepo.findByNumber("654321")).thenReturn(null);

        mockMvc.perform(post("/api/accounts/123456/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(content().string("Destination account not found"));

        verify(accountRepo, times(2)).findByNumber(anyString());
        verify(accountService, never()).transfer(anyString(), anyString(), anyDouble());
    }

    @Test
    void transfer_ShouldReturnValidationError_WhenPayloadInvalid() throws Exception {
        Map<String, Object> request = Map.of("amount", -20.0);

        mockMvc.perform(post("/api/accounts/123456/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation failed"))
                .andExpect(jsonPath("$.errors.toAccountNumber").value("Номер рахунку обов'язковий"));

        verify(accountService, never()).transfer(anyString(), anyString(), anyDouble());
    }
}
