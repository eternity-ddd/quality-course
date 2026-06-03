package org.eternity.food.shop.command.service;

import org.eternity.food.shop.command.domain.MenuConfiguration;

public record ChangeMenuConfigurationCommand(
        Long menuId,
        MenuConfiguration configuration
) {
    public Iterable<Long> optionGroupIds() {
        return configuration.optionGroupIds();
    }
}
