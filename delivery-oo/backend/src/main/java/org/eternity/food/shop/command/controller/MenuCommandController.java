package org.eternity.food.shop.command.controller;

import lombok.AllArgsConstructor;
import org.eternity.food.shop.command.service.MenuCommandService;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@AllArgsConstructor
public class MenuCommandController {
    private MenuCommandService menuCommandService;

    @PutMapping("/menus/{menuId}/open")
    public void open(@PathVariable("menuId") Long menuId) {
        menuCommandService.open(menuId);
    }

    @PutMapping("/menus/{menuId}/close")
    public void close(@PathVariable("menuId") Long menuId) {
        menuCommandService.close(menuId);
    }
}
