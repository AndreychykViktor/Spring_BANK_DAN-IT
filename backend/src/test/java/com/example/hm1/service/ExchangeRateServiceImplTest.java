package com.example.hm1.service;

import com.example.hm1.dto.ExchangeRateHistoryPoint;
import com.example.hm1.entity.Currency;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

class ExchangeRateServiceImplTest {

    private ExchangeRateServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ExchangeRateServiceImpl();
    }

    @Test
    void getExchangeRateReturnsOneForIdenticalCurrencies() {
        BigDecimal rate = service.getExchangeRate(Currency.UAH, Currency.UAH);
        assertThat(rate).isEqualByComparingTo(BigDecimal.ONE);
    }

    @Test
    void getExchangeRateCachesCalculatedValues() {
        BigDecimal firstCall = service.getExchangeRate(Currency.USD, Currency.EUR);
        BigDecimal secondCall = service.getExchangeRate(Currency.USD, Currency.EUR);

        assertAll(
                () -> assertThat(firstCall).isEqualByComparingTo(secondCall),
                () -> assertThat(firstCall).isPositive()
        );
    }

    @Test
    void convertAmountUsesExchangeRateAndRoundsToTwoDecimals() {
        BigDecimal converted = service.convertAmount(new BigDecimal("100.00"), Currency.USD, Currency.GBP);
        assertAll(
                () -> assertThat(converted.scale()).isEqualTo(2),
                () -> assertThat(converted).isPositive()
        );
    }

    @Test
    void getAllRatesReturnsCrossRatesForBaseCurrency() {
        Map<String, BigDecimal> rates = service.getAllRates(Currency.UAH);

        assertAll(
                () -> assertThat(rates)
                        .hasSize(Currency.values().length - 1)
                        .containsKeys("USD", "EUR", "GBP", "CHF"),
                () -> assertThat(rates.values()).allMatch(rate -> rate.compareTo(BigDecimal.ZERO) > 0)
        );
    }

    @Test
    void getHistoricalRatesGeneratesRequestedAmountOfPoints() {
        int days = 7;
        List<Currency> currencies = List.copyOf(EnumSet.of(Currency.USD, Currency.EUR, Currency.GBP));

        Map<String, List<ExchangeRateHistoryPoint>> history = service.getHistoricalRates(currencies, days);

        assertAll(
                () -> assertThat(history.keySet()).containsExactlyInAnyOrder("USD", "EUR", "GBP"),
                () -> assertThat(history.values()).allMatch(points -> points.size() == days),
                () -> assertThat(history.values())
                        .flatExtracting(points -> points)
                        .allSatisfy(point -> {
                            assertThat(point.getDate()).isNotBlank();
                            assertThat(point.getRate()).isNotNull();
                        })
        );
    }
}

