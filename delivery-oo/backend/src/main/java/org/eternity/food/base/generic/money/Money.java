package org.eternity.food.base.generic.money;

import org.eternity.food.base.domain.ValueObject;

import java.math.BigDecimal;

public class Money extends ValueObject<Money> implements Comparable<Money> {
    public static final Money ZERO = Money.wons(0);

    private final BigDecimal amount;

    public static Money wons(long amount) {
        return new Money(BigDecimal.valueOf(amount));
    }

    public static Money wons(double amount) {
        return new Money(BigDecimal.valueOf(amount));
    }

    Money(BigDecimal amount) {
        if (amount == null) {
            throw new IllegalArgumentException("금액은 null이어서는 안됩니다.");
        }

        this.amount = amount;
    }

    @Override
    protected Object[] getEqualityFields() {
        return new Object[] { amount.doubleValue() };
    }


    public Money plus(Money amount) {
        return new Money(this.amount.add(amount.amount));
    }

    public Money minus(Money amount) {
        return new Money(this.amount.subtract(amount.amount));
    }

    public Money times(double percent) {
        return new Money(this.amount.multiply(BigDecimal.valueOf(percent)));
    }

    public boolean isLessThan(Money other) {
        return amount.compareTo(other.amount) < 0;
    }

    public boolean isLessThanOrEqual(Money other) {
        return amount.compareTo(other.amount) <= 0;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public Long longValue() {
        return amount.longValue();
    }

    public String toString() {
        return amount.toString() + "원";
    }

    @Override
    public int compareTo(Money other) {
        return this.amount.compareTo(other.amount);
    }
}