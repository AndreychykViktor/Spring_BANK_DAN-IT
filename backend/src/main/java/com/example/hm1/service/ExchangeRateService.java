package com.example.hm1.service;

import com.example.hm1.dto.ExchangeRateHistoryPoint;
import com.example.hm1.entity.Currency;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
public interface ExchangeRateService {
    
    BigDecimal getExchangeRate(Currency from, Currency to);
    BigDecimal convertAmount(BigDecimal amount, Currency from, Currency to);
    Map<String, BigDecimal> getAllRates(Currency baseCurrency);
    Map<String, List<ExchangeRateHistoryPoint>> getHistoricalRates(List<Currency> currencies, int days);
    void updateExchangeRates();
}

