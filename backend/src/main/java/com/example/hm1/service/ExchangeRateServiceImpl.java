package com.example.hm1.service;

import com.example.hm1.dto.ExchangeRateHistoryPoint;
import com.example.hm1.entity.Currency;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Реалізація сервісу курсів валют
 * Наразі використовує статичні курси відносно UAH
 * TODO: Інтегрувати з реальним API (minfin.com.ua або exchangerate-api.com)
 */
@Service
public class ExchangeRateServiceImpl implements ExchangeRateService {

    // Зберігаємо курси в пам'яті (в майбутньому можна перенести в БД з timestamp)
    private final Map<String, BigDecimal> exchangeRates = new ConcurrentHashMap<>();
    
    // Базові курси відносно UAH (буде оновлюватись scheduler або ззовнішнього API)
    private static final Map<Currency, BigDecimal> BASE_RATES_UAH = new HashMap<>();
    
    static {
        // Курси на січень 2025 (приблизні)
        BASE_RATES_UAH.put(Currency.UAH, BigDecimal.ONE);
        BASE_RATES_UAH.put(Currency.USD, new BigDecimal("37.50"));  // 1 USD = 37.50 UAH
        BASE_RATES_UAH.put(Currency.EUR, new BigDecimal("40.80"));  // 1 EUR = 40.80 UAH
        BASE_RATES_UAH.put(Currency.GBP, new BigDecimal("47.60"));  // 1 GBP = 47.60 UAH
        BASE_RATES_UAH.put(Currency.CHF, new BigDecimal("43.20"));  // 1 CHF = 43.20 UAH
    }
    
    public ExchangeRateServiceImpl() {
        updateExchangeRates();
    }

    @Override
    public BigDecimal getExchangeRate(Currency from, Currency to) {
        // Якщо валюти однакові, повертаємо 1
        if (from == to) {
            return BigDecimal.ONE;
        }
        
        String rateKey = from.name() + "_TO_" + to.name();
        
        // Спочатку перевіряємо чи вже є прямий курс
        if (exchangeRates.containsKey(rateKey)) {
            return exchangeRates.get(rateKey);
        }
        
        // Якщо прямого курсу немає, розраховуємо через базову валюту UAH
        BigDecimal rate = calculateRateThroughBase(from, to);
        
        // Кешуємо розрахований курс
        exchangeRates.put(rateKey, rate);
        
        return rate;
    }

    @Override
    public BigDecimal convertAmount(BigDecimal amount, Currency from, Currency to) {
        BigDecimal rate = getExchangeRate(from, to);
        BigDecimal converted = amount.multiply(rate);
        
        // Округлюємо до 2 знаків після коми для грошових сум
        return converted.setScale(2, RoundingMode.HALF_UP);
    }

    @Override
    public Map<String, BigDecimal> getAllRates(Currency baseCurrency) {
        Map<String, BigDecimal> rates = new HashMap<>();
        
        for (Currency currency : Currency.values()) {
            if (currency != baseCurrency) {
                BigDecimal rate = getExchangeRate(baseCurrency, currency);
                rates.put(currency.name(), rate);
            }
        }
        
        return rates;
    }
    
    @Override
    public Map<String, List<ExchangeRateHistoryPoint>> getHistoricalRates(List<Currency> currencies, int days) {
        Map<String, List<ExchangeRateHistoryPoint>> history = new HashMap<>();
        if (currencies == null || currencies.isEmpty() || days <= 0) {
            return history;
        }

        LocalDate today = LocalDate.now();

        for (Currency currency : currencies) {
            if (currency == null || currency == Currency.UAH) {
                continue;
            }

            BigDecimal baseRate = BASE_RATES_UAH.get(currency);
            if (baseRate == null) {
                continue;
            }

            List<ExchangeRateHistoryPoint> points = new ArrayList<>();
            BigDecimal currentRate = baseRate;

            for (int i = days - 1; i >= 0; i--) {
                LocalDate date = today.minusDays(i);

                if (!points.isEmpty()) {
                    double variation = ThreadLocalRandom.current().nextDouble(-0.02, 0.02);
                    currentRate = currentRate.multiply(BigDecimal.valueOf(1 + variation))
                            .setScale(6, RoundingMode.HALF_UP);

                    if (currentRate.compareTo(BigDecimal.ZERO) <= 0) {
                        currentRate = baseRate;
                    }
                }

                BigDecimal roundedRate = currentRate.setScale(2, RoundingMode.HALF_UP);
                points.add(new ExchangeRateHistoryPoint(date.toString(), roundedRate));
            }

            history.put(currency.name(), points);
        }

        return history;
    }

    @Override
    public void updateExchangeRates() {
        System.out.println("ExchangeRateService: Updating exchange rates...");
        
        // Очищаємо старі курси
        exchangeRates.clear();
        
        // TODO: Отримати курси з реального API
        // Приклад з minfin.com.ua:
        // String url = "https://api.minfin.com.ua/mb/" + currency + "/";
        // або exchangerate-api.com:
        // String url = "https://api.exchangerate-api.com/v4/latest/UAH";
        
        // Поки що використовуємо статичні значення
        System.out.println("ExchangeRateService: Using mock rates");
        
        // Після отримання даних з API оновимо BASE_RATES_UAH
        // і автоматично розрахуються всі кроскурси
        
        System.out.println("ExchangeRateService: Exchange rates updated");
    }

    /**
     * Розрахунок курсу через базову валюту (UAH)
     * Приклад: USD -> EUR через UAH
     * 1 USD = 37.50 UAH
     * 1 EUR = 40.80 UAH
     * USD -> EUR = 37.50 / 40.80 = 0.9191 (1 USD = 0.9191 EUR)
     */
    private BigDecimal calculateRateThroughBase(Currency from, Currency to) {
        BigDecimal fromRate = BASE_RATES_UAH.get(from);
        BigDecimal toRate = BASE_RATES_UAH.get(to);
        
        if (fromRate == null || toRate == null) {
            throw new IllegalArgumentException("Exchange rate not found for: " + from + " or " + to);
        }
        
        // Конвертація через UAH: from -> UAH -> to
        return fromRate.divide(toRate, 6, RoundingMode.HALF_UP);
    }
}

