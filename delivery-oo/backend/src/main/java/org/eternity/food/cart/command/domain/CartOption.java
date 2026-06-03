package org.eternity.food.cart.command.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.eternity.food.base.domain.ValueObject;
import org.eternity.food.base.generic.money.Money;

@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PACKAGE)
public class CartOption extends ValueObject<CartOption> {

    @Column(name = "NAME")
    private String name;

    @Column(name = "PRICE")
    private Money price;

    @Builder
    public CartOption(String name, Money price) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name은 null이거나 비어 있어서는 안됩니다.");
        }

        this.name = name;
        this.price = price;
    }

    @Override
    protected Object[] getEqualityFields() {
        return new Object[] { name, price };
    }
}
