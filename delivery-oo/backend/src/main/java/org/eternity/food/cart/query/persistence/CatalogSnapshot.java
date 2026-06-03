package org.eternity.food.cart.query.persistence;

import java.util.List;
import java.util.Map;

/**
 * Cart 조회 시 *현재 시점*의 Menu/OptionGroup/Option 데이터.
 * Reconciler가 CartRaw와 비교한다.
 */
public record CatalogSnapshot(
        Map<Long, MenuInfo> menusById,
        Map<Long, OptionGroupInfo> optionGroupsById
) {
    public record MenuInfo(
            Long id,
            String name,
            String status,
            long basePrice,
            List<Long> optionGroupIds
    ) {
    }

    public record OptionGroupInfo(
            Long id,
            String name,
            Map<String, OptionInfo> optionsByName
    ) {
    }

    public record OptionInfo(
            String name,
            long price
    ) {
    }
}
