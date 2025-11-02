package com.example.hm1.controller;

import com.example.hm1.entity.Currency;
import com.example.hm1.service.ExchangeRateService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Контролер для роботи з курсами валют
 */
@RestController
@RequestMapping("/api/exchange-rates")
@CrossOrigin(origins = "*")
public class ExchangeRateController {

    private final ExchangeRateService exchangeRateService;

    public ExchangeRateController(ExchangeRateService exchangeRateService) {
        this.exchangeRateService = exchangeRateService;
    }

    /**
     * Отримати курс обміну між двома валютами
     * GET /api/exchange-rates?from=USD&to=EUR
     */
    @GetMapping
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<?> getExchangeRate(
            @RequestParam Currency from,
            @RequestParam Currency to) {
        try {
            BigDecimal rate = exchangeRateService.getExchangeRate(from, to);
            return ResponseEntity.ok(Map.of(
                "from", from.name(),
                "to", to.name(),
                "rate", rate
            ));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest()
                .body("Error getting exchange rate: " + e.getMessage());
        }
    }

    /**
     * Конвертувати суму з однієї валюти в іншу
     * GET /api/exchange-rates/convert?amount=100&from=USD&to=EUR
     */
    @GetMapping("/convert")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<?> convertAmount(
            @RequestParam BigDecimal amount,
            @RequestParam Currency from,
            @RequestParam Currency to) {
        try {
            BigDecimal converted = exchangeRateService.convertAmount(amount, from, to);
            return ResponseEntity.ok(Map.of(
                "originalAmount", amount,
                "originalCurrency", from.name(),
                "convertedAmount", converted,
                "convertedCurrency", to.name()
            ));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest()
                .body("Error converting amount: " + e.getMessage());
        }
    }

    /**
     * Отримати всі курси відносно базової валюти
     * GET /api/exchange-rates/all?base=UAH
     */
    @GetMapping("/all")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<?> getAllRates(@RequestParam(defaultValue = "UAH") Currency base) {
        try {
            Map<String, BigDecimal> rates = exchangeRateService.getAllRates(base);
            return ResponseEntity.ok(Map.of(
                "base", base.name(),
                "rates", rates
            ));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest()
                .body("Error getting all rates: " + e.getMessage());
        }
    }

    /**
     * Оновити курси валют (тільки для адміна)
     * POST /api/exchange-rates/update
     */
    @PostMapping("/update")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateRates() {
        try {
            exchangeRateService.updateExchangeRates();
            return ResponseEntity.ok("Exchange rates updated successfully");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest()
                .body("Error updating rates: " + e.getMessage());
        }
    }
}

