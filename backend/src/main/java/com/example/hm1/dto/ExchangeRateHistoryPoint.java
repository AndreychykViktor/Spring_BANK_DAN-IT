package com.example.hm1.dto;

import java.math.BigDecimal;

public class ExchangeRateHistoryPoint {
    private String date;
    private BigDecimal rate;

    public ExchangeRateHistoryPoint(String date, BigDecimal rate) {
        this.date = date;
        this.rate = rate;
    }

    public ExchangeRateHistoryPoint() {
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public BigDecimal getRate() {
        return rate;
    }

    public void setRate(BigDecimal rate) {
        this.rate = rate;
    }
}
