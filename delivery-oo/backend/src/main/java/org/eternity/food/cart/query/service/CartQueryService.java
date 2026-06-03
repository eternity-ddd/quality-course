package org.eternity.food.cart.query.service;

import org.eternity.food.cart.query.persistence.CartQueryDao;
import org.eternity.food.cart.query.persistence.CartRaw;
import org.eternity.food.cart.query.persistence.CartResponses;
import org.eternity.food.cart.query.persistence.CatalogSnapshot;
import org.eternity.food.shop.query.persistence.ShopResponses;
import org.eternity.food.shop.query.service.ShopQueryService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CartQueryService {
    private final CartQueryDao cartQueryDao;
    private final CartReconciler cartReconciler;
    private final ShopQueryService shopQueryService;

    public CartQueryService(CartQueryDao cartQueryDao,
                             CartReconciler cartReconciler,
                             ShopQueryService shopQueryService) {
        this.cartQueryDao = cartQueryDao;
        this.cartReconciler = cartReconciler;
        this.shopQueryService = shopQueryService;
    }

    @Transactional(readOnly = true)
    public CartResponses.Cart getCart(Long userId) {
        return cartQueryDao.findRawByUserId(userId)
                .map(this::reconcile)
                .orElse(null);
    }

    private CartResponses.Cart reconcile(CartRaw raw) {
        CatalogSnapshot catalog = cartQueryDao.loadCatalogFor(raw.menuIds());
        CartResponses.Cart.Shop shop = loadShop(raw.shopId());
        return cartReconciler.reconcile(raw, catalog, shop);
    }

    private CartResponses.Cart.Shop loadShop(Long shopId) {
        if (shopId == null) {
            return null;
        }
        ShopResponses.Brief brief = shopQueryService.findBriefById(shopId);
        return new CartResponses.Cart.Shop(
                brief.id(),
                brief.name(),
                0L,
                brief.minOrderAmount(),
                brief.open()
        );
    }
}
