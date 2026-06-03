package org.eternity.food.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Order의 items_snapshot JSON 한 칸. anemic POJO (Jackson 직렬화용).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderLineItem {

    private Long menuId;
    private String menuName;
    private Integer count;
    private Long unitPrice;
    @Builder.Default
    private List<OrderOptionGroup> groups = new ArrayList<>();

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class OrderOptionGroup {
        private String name;
        @Builder.Default
        private List<OrderOption> options = new ArrayList<>();
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class OrderOption {
        private String name;
        private Long price;
    }
}
