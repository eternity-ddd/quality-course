package org.eternity.food.dto;

import java.time.LocalDateTime;
import java.util.List;

public final class OrderDto {

    private OrderDto() {}

    public record OrderResponse(
            Long id,
            Long shopId,
            String shopName,
            LocalDateTime orderedTime,
            long totalPrice,
            List<LineItem> items
    ) {}

    public record LineItem(
            String menuName,
            int quantity,
            long unitPrice,
            long subtotal,
            List<Option> options
    ) {}

    public record Option(String groupName, String name, long price) {}
}
