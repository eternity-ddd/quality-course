package org.eternity.food.shop.command.service;

import lombok.AllArgsConstructor;
import org.eternity.food.shop.command.domain.Menu;
import org.eternity.food.shop.command.domain.MenuRepository;
import org.eternity.food.shop.command.domain.OptionGroup;
import org.eternity.food.shop.command.domain.OptionGroupRepository;
import org.eternity.food.shop.command.domain.SellableMenuInvariant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@AllArgsConstructor
public class MenuCommandService {
    private MenuRepository menuRepository;
    private OptionGroupRepository optionGroupRepository;

    @Transactional
    public void open(Long menuId) {
        Menu menu = menuRepository.findById(menuId)
                .orElseThrow(() -> new IllegalArgumentException("메뉴를 찾을 수 없습니다: " + menuId));

        if (!menu.canOpen()) {
            return;
        }

        List<OptionGroup> optionGroups = optionGroupRepository.findAllById(menu.getOptionGroupIds());
        menu.open(new SellableMenuInvariant(optionGroups));

        menuRepository.save(menu);
    }

    @Transactional
    public void close(Long menuId) {
        Menu menu = menuRepository.findById(menuId)
                .orElseThrow(() -> new IllegalArgumentException("메뉴를 찾을 수 없습니다: " + menuId));

        if (!menu.canClose()) {
            return;
        }

        menu.close();

        menuRepository.save(menu);
    }

    @Transactional
    public void changeMenuConfiguration(ChangeMenuConfigurationCommand command) {
        Menu menu = menuRepository.findById(command.menuId())
                .orElseThrow(() -> new IllegalArgumentException("메뉴를 찾을 수 없습니다: " + command.menuId()));

        List<OptionGroup> optionGroups = optionGroupRepository.findAllById(command.optionGroupIds());
        menu.changeConfiguration(command.configuration(), new SellableMenuInvariant(optionGroups));

        menuRepository.save(menu);
    }
}
