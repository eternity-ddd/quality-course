package org.eternity.food.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 주문. @Table(name = "orders")로 SQL 예약어 회피.
 * items_snapshot은 JSON 컬럼.
 */
@Entity
@Table(name = "orders")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "shop_id")
    private Long shopId;

    @Column(name = "ordered_time")
    private LocalDateTime orderedTime;

    @Column(name = "total_price")
    private Long totalPrice;

    @Builder.Default
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "items_snapshot", columnDefinition = "JSON")
    private List<OrderLineItem> itemsSnapshot = new ArrayList<>();
}
