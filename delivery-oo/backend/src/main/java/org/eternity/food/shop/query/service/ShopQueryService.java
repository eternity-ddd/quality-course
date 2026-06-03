package org.eternity.food.shop.query.service;

import org.eternity.food.shop.query.persistence.ShopQueryDao;
import org.eternity.food.shop.query.persistence.ShopResponses;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ShopQueryService {
    private final ShopQueryDao shopQueryDao;

    public ShopQueryService(ShopQueryDao shopQueryDao) {
        this.shopQueryDao = shopQueryDao;
    }

    @Transactional(readOnly = true)
    public ShopResponses.Detail findById(Long shopId) {
        return shopQueryDao.findById(shopId)
                .orElseThrow(() -> new IllegalArgumentException("가게를 찾을 수 없습니다: " + shopId));
    }

    @Transactional(readOnly = true)
    public ShopResponses.Brief findBriefById(Long shopId) {
        return shopQueryDao.findBriefById(shopId)
                .orElseThrow(() -> new IllegalArgumentException("가게를 찾을 수 없습니다: " + shopId));
    }

    @Transactional(readOnly = true)
    public Slice<ShopResponses.Nearby> findNearby(double lat, double lng, String category, Pageable pageable) {
        String categoryCode = hasCategory(category) ? ShopResponses.CategoryNames.toCode(category) : null;
        return shopQueryDao.findNearby(lat, lng, categoryCode, pageable);
    }

    private boolean hasCategory(String category) {
        return category != null && !category.isBlank() && !"전체".equals(category);
    }
}
