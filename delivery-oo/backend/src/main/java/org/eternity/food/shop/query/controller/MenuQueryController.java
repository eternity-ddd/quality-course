package org.eternity.food.shop.query.controller;

import lombok.AllArgsConstructor;
import org.eternity.food.shop.query.persistence.MenuResponses;
import org.eternity.food.shop.query.service.MenuQueryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@AllArgsConstructor
public class MenuQueryController {
    private MenuQueryService menuQueryService;

    @GetMapping("/api/shops/{shopId}/menus")
    public List<MenuResponses.Item> list(@PathVariable Long shopId) {
        return menuQueryService.findByShopId(shopId);
    }

    @GetMapping("/api/shops/{shopId}/menus/{menuId}")
    public MenuResponses.Detail detail(@PathVariable Long shopId, @PathVariable Long menuId) {
        return menuQueryService.getMenuDetail(menuId);
    }
}
