package com.example.hm1.service.chat;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ChatCommandParser {

    private static final Pattern CASH_COMMAND = Pattern.compile("^\\$cash\\s+(\\d+(?:\\.\\d{1,2})?)\\s+([A-Za-z]{3})\\s*->\\s*$");

    public Optional<CashCommand> parse(String message) {
        if (message == null) {
            return Optional.empty();
        }

        Matcher matcher = CASH_COMMAND.matcher(message.trim());
        if (!matcher.matches()) {
            return Optional.empty();
        }

        BigDecimal amount = new BigDecimal(matcher.group(1));
        String currencyCode = matcher.group(2).toUpperCase(Locale.ROOT);
        return Optional.of(new CashCommand(amount, currencyCode));
    }

    public static class CashCommand {
        private final BigDecimal amount;
        private final String currency;

        public CashCommand(BigDecimal amount, String currency) {
            this.amount = amount;
            this.currency = currency;
        }

        public BigDecimal getAmount() {
            return amount;
        }

        public String getCurrency() {
            return currency;
        }
    }
}

