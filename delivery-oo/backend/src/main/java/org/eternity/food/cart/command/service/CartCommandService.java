package org.eternity.food.cart.command.service;

import lombok.AllArgsConstructor;
import org.eternity.food.cart.command.domain.Cart;
import org.eternity.food.cart.command.domain.CartLineItem;
import org.eternity.food.cart.command.domain.CartRepository;
import org.eternity.food.shop.command.domain.Menu;
import org.eternity.food.shop.command.domain.MenuRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@AllArgsConstructor
public class CartCommandService {
    private CartRepository cartRepository;
    private MenuRepository menuRepository;

    @Transactional
    public void addCartLineItem(Long userId, CartLineItemCommand command) {
        Cart cart = findCart(userId);
        Menu menu = menuRepository.findById(command.menuId())
                .orElseThrow(() -> new IllegalArgumentException("메뉴를 찾을 수 없습니다: " + command.menuId()));

        CartLineItem item = CartLineItemMapper.map(menu, command);
        cart.addItem(menu.getShopId(), item);
        cartRepository.save(cart);
    }

    @Transactional
    public void updateItemQuantity(Long userId, Long itemId, int quantity) {
        Cart cart = findCart(userId);
        cart.changeItemQuantity(itemId, quantity);
        cartRepository.save(cart);
    }

    @Transactional
    public void removeItem(Long userId, Long itemId) {
        Cart cart = findCart(userId);
        cart.removeItem(itemId);
        cartRepository.save(cart);
    }

    private Cart findCart(Long userId) {
        return cartRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("'" + userId.longValue() + "'의 카트는 존재하지 않는 사용자 ID입니다."));
    }
}
