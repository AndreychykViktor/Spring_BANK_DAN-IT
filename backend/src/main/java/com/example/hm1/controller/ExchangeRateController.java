package com.example.hm1.controller;

import com.example.hm1.dto.ExchangeRateHistoryPoint;
import com.example.hm1.entity.Currency;
import com.example.hm1.service.ExchangeRateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Контролер для роботи з курсами валют
 */
@RestController
@RequestMapping("/api/exchange-rates")
@CrossOrigin(origins = "*")
@io.swagger.v3.oas.annotations.tags.Tag(name = "Exchange Rates", description = "API для роботи з курсами валют та конвертацією")
public class ExchangeRateController {

    private final ExchangeRateService exchangeRateService;

    public ExchangeRateController(ExchangeRateService exchangeRateService) {
        this.exchangeRateService = exchangeRateService;
    }

    @GetMapping
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    @Operation(summary = "Отримати курс обміну валют", description = "Повертає поточний курс обміну між двома валютами")
    @ApiResponse(responseCode = "200", description = "Успішно отримано курс обміну")
    public ResponseEntity<?> getExchangeRate(
            @Parameter(description = "Вихідна валюта", required = true)
            @RequestParam Currency from,
            @Parameter(description = "Цільова валюта", required = true)
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

    @GetMapping("/convert")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    @Operation(summary = "Конвертувати суму", description = "Конвертує вказану суму з однієї валюти в іншу за поточним курсом")
    @ApiResponse(responseCode = "200", description = "Конвертацію виконано успішно")
    public ResponseEntity<?> convertAmount(
            @Parameter(description = "Сума для конвертації", required = true)
            @RequestParam BigDecimal amount,
            @Parameter(description = "Вихідна валюта", required = true)
            @RequestParam Currency from,
            @Parameter(description = "Цільова валюта", required = true)
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
    @GetMapping("/history")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<?> getRateHistory(
            @RequestParam(required = false) String currencies,
            @RequestParam(defaultValue = "7d") String period) {
        try {
            List<Currency> requestedCurrencies = parseCurrencies(currencies);
            int days = resolvePeriodToDays(period);

            Map<String, List<ExchangeRateHistoryPoint>> history = exchangeRateService.getHistoricalRates(requestedCurrencies, days);
            return ResponseEntity.ok(Map.of(
                "period", period,
                "days", days,
                "history", history
            ));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest()
                .body("Error parsing request: " + ex.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest()
                .body("Error getting rate history: " + e.getMessage());
        }
    }

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

    private List<Currency> parseCurrencies(String currencies) {
        List<Currency> defaults = List.of(Currency.USD, Currency.EUR, Currency.GBP);
        if (currencies == null || currencies.isBlank()) {
            return defaults;
        }

        List<Currency> result = new ArrayList<>();
        String[] parts = currencies.split(",");
        for (String part : parts) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) {
                result.add(Currency.valueOf(trimmed.toUpperCase()));
            }
        }

        return result.isEmpty() ? defaults : result;
    }

    private int resolvePeriodToDays(String period) {
        if (period == null) {
            return 7;
        }

        String normalized = period.trim().toLowerCase();
        switch (normalized) {
            case "14d":
                return 14;
            case "1m":
                return 30;
            case "3m":
                return 90;
            case "6m":
                return 180;
            case "12m":
                return 365;
            case "7d":
            default:
                if (normalized.endsWith("d")) {
                    String value = normalized.substring(0, normalized.length() - 1);
                    return Math.max(Integer.parseInt(value), 1);
                }
                return 7;
        }
    }
}

