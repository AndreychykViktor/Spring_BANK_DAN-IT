package com.example.hm1.service;

import com.example.hm1.dto.ExchangeRateHistoryPoint;
import com.example.hm1.entity.Currency;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Сервіс для роботи з курсами валют
 */
public interface ExchangeRateService {
    
    /**
     * Отримати актуальний курс обміну між двома валютами
     * @param from валюта з якої конвертуємо
     * @param to валюта в яку конвертуємо
     * @return курс обміну (скільки 'to' можна отримати за 1 'from')
     */
    BigDecimal getExchangeRate(Currency from, Currency to);
    
    /**
     * Конвертувати суму з однієї валюти в іншу
     * @param amount сума для конвертації
     * @param from валюта з якої конвертуємо
     * @param to валюта в яку конвертуємо
     * @return конвертована сума
     */
    BigDecimal convertAmount(BigDecimal amount, Currency from, Currency to);
    
    /**
     * Отримати всі актуальні курси відносно базової валюти
     * @param baseCurrency базова валюта (за замовчуванням UAH)
     * @return Map з курсами
     */
    Map<String, BigDecimal> getAllRates(Currency baseCurrency);
    
    /**
     * Отримати історію курсу валют за вказаний період
     * @param currencies список валют для отримання історії
     * @param days кількість днів у періоді
     * @return Map з історичними даними для кожної валюти
     */
    Map<String, List<ExchangeRateHistoryPoint>> getHistoricalRates(List<Currency> currencies, int days);
    
    /**
     * Оновити курси валют (викликається scheduler або вручну)
     */
    void updateExchangeRates();
}

