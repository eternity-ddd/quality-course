package org.eternity.food.cart.command.controller;

import org.eternity.food.cart.command.service.CartCommandService;
import org.eternity.food.cart.command.service.CartLineItemCommand;
import org.eternity.food.cart.command.service.CartLineItemCommand.CartOptionCommand;
import org.eternity.food.cart.command.service.CartLineItemCommand.CartOptionGroupCommand;
import org.eternity.food.cart.command.service.PlaceOrderService;
import org.eternity.food.cart.query.persistence.CartResponses;
import org.eternity.food.cart.query.service.CartQueryService;
import org.eternity.food.order.command.domain.Order;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/cart")
public class CartCommandController {
    private static final Long DEMO_USER_ID = 1L;

    private final CartCommandService cartCommandService;
    private final PlaceOrderService placeOrderService;
    private final CartQueryService cartQueryService;

    public CartCommandController(CartCommandService cartCommandService,
                                 PlaceOrderService placeOrderService,
                                 CartQueryService cartQueryService) {
        this.cartCommandService = cartCommandService;
        this.placeOrderService = placeOrderService;
        this.cartQueryService = cartQueryService;
    }

    @PostMapping("/items")
    public CartResponses.Cart addItem(@RequestBody CartItemRequest request) {
        cartCommandService.addCartLineItem(DEMO_USER_ID, request.toCommand());
        return loadCart(request.sessionId());
    }

    @PatchMapping("/items/{itemId}")
    public CartResponses.Cart updateQuantity(@PathVariable Long itemId,
                                              @RequestBody Map<String, Integer> body,
                                              @RequestParam(required = false) String sessionId) {
        cartCommandService.updateItemQuantity(DEMO_USER_ID, itemId, body.get("quantity"));
        return loadCart(sessionId);
    }

    @DeleteMapping("/items/{itemId}")
    public CartResponses.Cart removeItem(@PathVariable Long itemId,
                                          @RequestParam(required = false) String sessionId) {
        cartCommandService.removeItem(DEMO_USER_ID, itemId);
        return loadCart(sessionId);
    }

    @PostMapping("/order")
    public OrderPlacedResponse placeOrder(@RequestParam(required = false) String sessionId) {
        Order order = placeOrderService.placeOrder(DEMO_USER_ID);
        return new OrderPlacedResponse(order.getId(), order.getTotalPrice().longValue());
    }

    private CartResponses.Cart loadCart(String sessionId) {
        CartResponses.Cart cart = cartQueryService.getCart(DEMO_USER_ID);
        if (cart == null) {
            return CartResponses.Cart.empty(sessionId);
        }
        return cart.withSessionId(sessionId);
    }
}

record OrderPlacedResponse(Long orderId, long totalPrice) {}

record CartItemRequest(
        String sessionId,
        Long menuId,
        String menuName,
        Integer quantity,
        List<SelectedOption> selectedOptions
) {
    record SelectedOption(Long optionGroupId, String optionGroupName, String name, Long price) {}

    CartLineItemCommand toCommand() {
        Map<Long, List<SelectedOption>> grouped = new LinkedHashMap<>();
        if (selectedOptions != null) {
            for (SelectedOption option : selectedOptions) {
                grouped.computeIfAbsent(option.optionGroupId(), k -> new java.util.ArrayList<>()).add(option);
            }
        }

        List<CartOptionGroupCommand> groups = grouped.entrySet().stream()
                .map(entry -> {
                    SelectedOption first = entry.getValue().get(0);
                    return new CartOptionGroupCommand(
                            entry.getKey(),
                            first.optionGroupName(),
                            entry.getValue().stream()
                                    .map(opt -> new CartOptionCommand(opt.name(), opt.price()))
                                    .toList());
                })
                .toList();

        return new CartLineItemCommand(menuId, menuName, quantity, groups);
    }
}
