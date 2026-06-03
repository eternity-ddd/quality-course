package org.eternity.food.cart.command.service;

import java.util.List;

public record CartLineItemCommand(
    Long menuId,
    String menuName,
    Integer count,
    List<CartOptionGroupCommand> optionGroups
) {
    public record CartOptionGroupCommand(
            Long optionGroupId,
            String optionGroupName,
            List<CartOptionCommand> options
    ) {
    }

    public record CartOptionCommand(
            String name,
            Long price
    ) {
    }
}
