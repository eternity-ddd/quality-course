package org.eternity.food.cart.query.controller;

import org.eternity.food.cart.query.persistence.CartResponses;
import org.eternity.food.cart.query.service.CartQueryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/cart")
public class CartQueryController {
    private static final Long DEMO_USER_ID = 1L;

    private final CartQueryService cartQueryService;

    public CartQueryController(CartQueryService cartQueryService) {
        this.cartQueryService = cartQueryService;
    }

    @GetMapping
    public CartResponses.Cart getCart(@RequestParam(required = false) String sessionId) {
        CartResponses.Cart cart = cartQueryService.getCart(DEMO_USER_ID);
        if (cart == null) {
            return CartResponses.Cart.empty(sessionId);
        }
        return cart.withSessionId(sessionId);
    }
}
