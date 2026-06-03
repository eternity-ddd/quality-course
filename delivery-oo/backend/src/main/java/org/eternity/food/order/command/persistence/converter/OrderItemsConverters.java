package org.eternity.food.order.command.persistence.converter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.eternity.food.order.command.domain.OrderItems;
import org.eternity.food.order.command.domain.OrderLineItem;

import java.util.List;

@Converter
public class OrderItemsConverters implements AttributeConverter<OrderItems, String> {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(OrderItems attribute) {
        if (attribute == null) {
            return null;
        }
        try {
            return MAPPER.writeValueAsString(attribute.list());
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("OrderItems 직렬화 실패", e);
        }
    }

    @Override
    public OrderItems convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        try {
            List<OrderLineItem> items = MAPPER.readValue(dbData,
                    MAPPER.getTypeFactory().constructCollectionType(List.class, OrderLineItem.class));
            return new OrderItems(items);
        } catch (Exception e) {
            throw new IllegalStateException("OrderItems 역직렬화 실패", e);
        }
    }
}
