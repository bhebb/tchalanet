package com.tchalanet.server.common.types.money;

import java.math.BigDecimal;
import java.util.Objects;

public record Money(
    BigDecimal amount,
    CurrencyCode currency
) {
    public Money {
        Objects.requireNonNull(amount, "amount is required");
        Objects.requireNonNull(currency, "currency is required");

        if (amount.signum() < 0) {
            throw new IllegalArgumentException("money amount must be >= 0");
        }

        amount = amount.stripTrailingZeros();
    }

    public static Money zero(CurrencyCode currency) {
        return new Money(BigDecimal.ZERO, currency);
    }

    public Money plus(Money other) {
        requireSameCurrency(other);
        return new Money(amount.add(other.amount), currency);
    }

    public Money minus(Money other) {
        requireSameCurrency(other);
        var result = amount.subtract(other.amount);
        if (result.signum() < 0) {
            throw new IllegalArgumentException("money result must be >= 0");
        }
        return new Money(result, currency);
    }

    public boolean isZero() {
        return amount.signum() == 0;
    }

    private void requireSameCurrency(Money other) {
        if (!currency.equals(other.currency())) {
            throw new IllegalArgumentException("currency mismatch");
        }
    }
}
