package org.eternity.food.order.command.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.eternity.food.Fixtures.anOrderLineItem;

class OrderItemsTest {

    @Test
    @DisplayName("of(null)이면 빈 list로 wrap")
    void of_null_isEmpty() {
        OrderItems items = OrderItems.of(null);

        assertThat(items.list()).isEmpty();
    }

    @Test
    @DisplayName("of(items)는 들어온 items를 보존")
    void of_preservesItems() {
        OrderLineItem line = anOrderLineItem();

        OrderItems items = OrderItems.of(List.of(line));

        assertThat(items.list()).containsExactly(line);
    }

    @Test
    @DisplayName("list()는 unmodifiable view")
    void list_isUnmodifiable() {
        OrderItems items = OrderItems.of(List.of(anOrderLineItem()));

        assertThatThrownBy(() -> items.list().add(anOrderLineItem()))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("생성자가 defensive copy: 원본 변경이 내부에 영향 없음")
    void constructor_defensiveCopy() {
        List<OrderLineItem> mutable = new ArrayList<>();
        mutable.add(anOrderLineItem());
        OrderItems items = OrderItems.of(mutable);

        mutable.clear();

        assertThat(items.list()).hasSize(1);
    }

    @Test
    @DisplayName("record 동등성: 같은 items면 equal")
    void record_equality() {
        OrderLineItem line = anOrderLineItem();
        OrderItems a = OrderItems.of(List.of(line));
        OrderItems b = OrderItems.of(List.of(line));

        assertThat(a).isEqualTo(b);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
    }

    @Test
    @DisplayName("record 동등성: 다른 items면 not equal")
    void record_inequality() {
        OrderItems a = OrderItems.of(List.of(anOrderLineItem()));
        OrderItems b = OrderItems.of(List.of());

        assertThat(a).isNotEqualTo(b);
    }
}
