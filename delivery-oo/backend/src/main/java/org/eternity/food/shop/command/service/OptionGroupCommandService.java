package org.eternity.food.shop.command.service;

import lombok.AllArgsConstructor;
import org.eternity.food.shop.command.domain.OptionGroup;
import org.eternity.food.shop.command.domain.OptionGroupChangeFlow;
import org.eternity.food.shop.command.domain.OptionGroupRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@AllArgsConstructor
public class OptionGroupCommandService {
    private OptionGroupRepository optionGroupRepository;
    private OptionGroupChangeFlow optionGroupChangeFlow;

    @Transactional
    public void updateOptions(UpdateOptionsCommand command) {
        OptionGroup optionGroup = optionGroupRepository.findById(command.optionGroupId())
                .orElseThrow(() -> new IllegalArgumentException("OptionGroup not found: " + command.optionGroupId()));

        optionGroupChangeFlow.updateOptions(optionGroup, command.patches());
        optionGroupRepository.save(optionGroup);
    }
}
