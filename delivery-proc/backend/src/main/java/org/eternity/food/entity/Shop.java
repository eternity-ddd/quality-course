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

import java.time.LocalTime;

/**
 * 가게. JPA anemic entity. 동작 없음. Getter/Setter만 노출.
 *
 * <p>LOCATION POINT 컬럼은 일부러 매핑하지 않는다. (data.sql 시드만 사용하고 JPA로 INSERT 안 함)
 */
@Entity
@Table(name = "shop")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Shop {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @Column(name = "min_order_price")
    private Long minOrderPrice;

    @Column(name = "start_time")
    private LocalTime startTime;

    @Column(name = "end_time")
    private LocalTime endTime;

    private String category;

    private Double latitude;

    private Double longitude;

    @Column(name = "delivery_radius")
    private Double deliveryRadius;
}
