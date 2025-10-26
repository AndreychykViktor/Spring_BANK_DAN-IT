package com.example.hm1.controller;

import com.example.hm1.entity.Customer;
import com.example.hm1.service.CustomerService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CustomerController.class)
class CustomerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CustomerService customerService;

    @Autowired
    private ObjectMapper objectMapper;

    private Customer testCustomer;

    @BeforeEach
    void setUp() {
        testCustomer = new Customer("John Doe", "john@example.com", 30);
        testCustomer.setId(1L);
    }

    @Test
    void getAllCustomers_ShouldReturnCustomersList() throws Exception {
        // Given
        List<Customer> customers = Arrays.asList(testCustomer);
        when(customerService.getAllCustomers()).thenReturn(customers);

        // When & Then
        mockMvc.perform(get("/api/customers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].name").value("John Doe"))
                .andExpect(jsonPath("$[0].email").value("john@example.com"));

        verify(customerService).getAllCustomers();
    }

    @Test
    void getCustomerById_ShouldReturnCustomer_WhenExists() throws Exception {
        // Given
        when(customerService.getCustomerById(1L)).thenReturn(testCustomer);

        // When & Then
        mockMvc.perform(get("/api/customers/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("John Doe"))
                .andExpect(jsonPath("$.email").value("john@example.com"));

        verify(customerService).getCustomerById(1L);
    }

    @Test
    void getCustomerById_ShouldReturnNotFound_WhenNotExists() throws Exception {
        // Given
        when(customerService.getCustomerById(999L)).thenReturn(null);

        // When & Then
        mockMvc.perform(get("/api/customers/999"))
                .andExpect(status().isNotFound());

        verify(customerService).getCustomerById(999L);
    }

    @Test
    void createCustomer_ShouldReturnCreatedCustomer_WhenValidData() throws Exception {
        // Given
        Map<String, Object> customerData = Map.of(
                "name", "Jane Doe",
                "email", "jane@example.com",
                "age", 25
        );

        Customer newCustomer = new Customer("Jane Doe", "jane@example.com", 25);
        newCustomer.setId(2L);

        when(customerService.createCustomer("Jane Doe", 25)).thenReturn(newCustomer);

        // When & Then
        mockMvc.perform(post("/api/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(customerData)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(2))
                .andExpect(jsonPath("$.name").value("Jane Doe"))
                .andExpect(jsonPath("$.email").value("jane@example.com"));

        verify(customerService).createCustomer("Jane Doe", 25);
    }

    @Test
    void createCustomer_ShouldReturnBadRequest_WhenInvalidData() throws Exception {
        // Given
        Map<String, Object> invalidData = Map.of(
                "name", "Invalid Customer"
                // Missing required fields
        );

        when(customerService.createCustomer(any(), any())).thenThrow(new RuntimeException("Invalid data"));

        // When & Then
        mockMvc.perform(post("/api/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidData)))
                .andExpect(status().isBadRequest());

        verify(customerService, never()).createCustomer(any(), any());
    }
}
