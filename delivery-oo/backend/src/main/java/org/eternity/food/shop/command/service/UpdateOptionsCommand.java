package org.eternity.food.shop.command.service;

import org.eternity.food.shop.command.domain.OptionPatch;

import java.util.List;

public record UpdateOptionsCommand(
        Long optionGroupId,
        List<OptionPatch> patches
) {
}
