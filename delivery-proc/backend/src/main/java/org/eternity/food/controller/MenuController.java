package org.eternity.food.controller;

import org.eternity.food.dto.MenuDto;
import org.eternity.food.service.MenuService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class MenuController {

    private final MenuService menuService;

    public MenuController(MenuService menuService) {
        this.menuService = menuService;
    }

    @GetMapping("/api/shops/{shopId}/menus")
    public List<MenuDto.Item> list(@PathVariable Long shopId) {
        return menuService.findByShopId(shopId);
    }

    @GetMapping("/api/shops/{shopId}/menus/{menuId}")
    public MenuDto.Detail detail(@PathVariable Long shopId, @PathVariable Long menuId) {
        return menuService.findDetail(menuId);
    }
}
