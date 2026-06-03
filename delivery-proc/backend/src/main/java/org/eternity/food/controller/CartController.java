package org.eternity.food.controller;

import org.eternity.food.dto.CartDto;
import org.eternity.food.service.CartService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 카트 단일 controller. GET/POST/PATCH/DELETE + 주문 전환까지 한 클래스에 모임.
 */
@RestController
@RequestMapping("/api/cart")
public class CartController {

    private static final Long DEMO_USER_ID = 1L;

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @GetMapping
    public CartDto.CartResponse getCart(@RequestParam(required = false) String sessionId) {
        return cartService.getCart(DEMO_USER_ID, sessionId);
    }

    @GetMapping("/total-price")
    public long getTotalPrice() {
        return cartService.getTotalPrice(DEMO_USER_ID);
    }

    @PostMapping("/items")
    public CartDto.CartResponse addItem(@RequestBody CartDto.AddItemRequest request) {
        return cartService.addItem(DEMO_USER_ID, request);
    }

    @PatchMapping("/items/{itemId}")
    public CartDto.CartResponse updateQuantity(@PathVariable Long itemId,
                                               @RequestBody CartDto.UpdateQuantityRequest body,
                                               @RequestParam(required = false) String sessionId) {
        return cartService.updateQuantity(DEMO_USER_ID, itemId, body.quantity(), sessionId);
    }

    @DeleteMapping("/items/{itemId}")
    public CartDto.CartResponse removeItem(@PathVariable Long itemId,
                                           @RequestParam(required = false) String sessionId) {
        return cartService.removeItem(DEMO_USER_ID, itemId, sessionId);
    }

    @PostMapping("/order")
    public CartDto.OrderPlacedResponse placeOrder(@RequestParam(required = false) String sessionId) {
        return cartService.placeOrder(DEMO_USER_ID);
    }
}
