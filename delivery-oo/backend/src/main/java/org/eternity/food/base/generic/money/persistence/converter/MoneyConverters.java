package org.eternity.food.base.generic.money.persistence.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.eternity.food.base.generic.money.Money;

@Converter(autoApply = true)
public class MoneyConverters implements AttributeConverter<Money, Long> {

    @Override
    public Long convertToDatabaseColumn(Money attribute) {
        return attribute == null ? null : attribute.longValue();
    }

    @Override
    public Money convertToEntityAttribute(Long dbData) {
        return dbData == null ? null : Money.wons(dbData);
    }
}
