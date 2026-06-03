package org.eternity.food.order.command.domain;

import java.util.Collections;
import java.util.List;

/**
 * JSON으로 직렬화되는 OrderLineItem 컬렉션 래퍼.
 *
 * <p>Spring Data JDBC는 generic type erasure 때문에 {@code List<OrderLineItem>} 자체에 컨버터를
 * 매핑할 수 없어 *구체적인 래퍼 타입*이 필요. 여기서는 JSON 직렬화의 단위가 되는 VO 역할.
 */
public record OrderItems(List<OrderLineItem> items) {
    public OrderItems(List<OrderLineItem> items) {
        this.items = items == null ? List.of() : List.copyOf(items);
    }

    public static OrderItems of(List<OrderLineItem> items) {
        return new OrderItems(items);
    }

    public List<OrderLineItem> list() {
        return Collections.unmodifiableList(items);
    }
}
