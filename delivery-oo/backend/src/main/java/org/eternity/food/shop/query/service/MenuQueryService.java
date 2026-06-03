package org.eternity.food.shop.query.service;

import org.eternity.food.shop.query.persistence.MenuQueryDao;
import org.eternity.food.shop.query.persistence.MenuResponses;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class MenuQueryService {
    private final MenuQueryDao menuQueryDao;

    public MenuQueryService(MenuQueryDao menuQueryDao) {
        this.menuQueryDao = menuQueryDao;
    }

    @Transactional(readOnly = true)
    public List<MenuResponses.Item> findByShopId(Long shopId) {
        return menuQueryDao.findByShopId(shopId);
    }

    @Transactional(readOnly = true)
    public MenuResponses.Detail getMenuDetail(Long menuId) {
        return menuQueryDao.findDetail(menuId)
                .orElseThrow(() -> new IllegalArgumentException("메뉴를 찾을 수 없습니다: " + menuId));
    }
}
