package org.eternity.food.cart.command.service;

import lombok.AllArgsConstructor;
import org.eternity.food.cart.command.domain.Cart;
import org.eternity.food.cart.command.domain.CartLineItem;
import org.eternity.food.cart.command.domain.CartOption;
import org.eternity.food.cart.command.domain.CartOptionGroup;
import org.eternity.food.cart.command.domain.CartRepository;
import org.eternity.food.cart.command.domain.OrderableCartInvariant;
import org.eternity.food.order.command.domain.Order;
import org.eternity.food.order.command.domain.OrderLineItem;
import org.eternity.food.order.command.domain.OrderRepository;
import org.eternity.food.shop.command.domain.Menu;
import org.eternity.food.shop.command.domain.MenuRepository;
import org.eternity.food.shop.command.domain.OptionGroup;
import org.eternity.food.shop.command.domain.OptionGroupRepository;
import org.eternity.food.shop.command.domain.Shop;
import org.eternity.food.shop.command.domain.ShopRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;

/**
 * Cart → Order 변환을 책임지는 *cart 측 orchestration*.
 *
 * <p>DDD 정통 그림에서는 *Order aggregate가 Cart aggregate를 모름*. 따라서 변환 책임은
 * "주문하기" 행위의 출발점인 cart 패키지에 둔다 (Cart → Order의 단방향 의존, SDP 부합).
 *
 * <p>Order 생성자는 순수 데이터(userId, shopId, OrderLineItem 리스트)만 받음. Cart의 내부
 * 구조는 이 서비스에서 OrderLineItem으로 변환되어 들어간다.
 */
@Service
@AllArgsConstructor
public class PlaceOrderService {
    private final CartRepository cartRepository;
    private final ShopRepository shopRepository;
    private final MenuRepository menuRepository;
    private final OptionGroupRepository optionGroupRepository;
    private final OrderRepository orderRepository;

    @Transactional
    public Order placeOrder(Long userId) {
        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("'" + userId.longValue() + "'의 카트는 존재하지 않는 사용자 ID입니다."));

        if (cart.getShopId() == null) {
            throw new IllegalStateException("장바구니가 비어 있어 주문할 수 없습니다.");
        }

        Shop shop = shopRepository.findById(cart.getShopId())
                .orElseThrow(() -> new IllegalArgumentException("가게를 찾을 수 없습니다: " + cart.getShopId()));

        List<Menu> menus = menuRepository.findAllById(menuIdsOf(cart));
        List<OptionGroup> optionGroups = optionGroupRepository.findAllById(optionGroupIdsOf(menus));

        new OrderableCartInvariant(shop, menus, optionGroups).check(cart).require();

        List<OrderLineItem> items = cart.getItems().stream()
                .map(PlaceOrderService::toLineItem)
                .toList();

        Order order = new Order(cart.getUserId(), cart.getShopId(), items);
        return orderRepository.save(order);
    }

    private List<Long> menuIdsOf(Cart cart) {
        return cart.getItems().stream()
                .map(CartLineItem::getMenuId)
                .distinct()
                .toList();
    }

    private List<Long> optionGroupIdsOf(Collection<Menu> menus) {
        return menus.stream()
                .flatMap(menu -> menu.getOptionGroupIds().stream())
                .distinct()
                .toList();
    }

    private static OrderLineItem toLineItem(CartLineItem item) {
        return new OrderLineItem(
                item.getMenuId(),
                item.getMenuName(),
                item.getMenuCount(),
                item.getBasePrice().longValue(),
                item.getGroups().stream()
                        .map(PlaceOrderService::toOptionGroup)
                        .toList()
        );
    }

    private static OrderLineItem.OrderOptionGroup toOptionGroup(CartOptionGroup group) {
        return new OrderLineItem.OrderOptionGroup(
                group.getName(),
                group.getOptions().stream()
                        .map(PlaceOrderService::toOption)
                        .toList()
        );
    }

    private static OrderLineItem.OrderOption toOption(CartOption option) {
        return new OrderLineItem.OrderOption(option.getName(), option.getPrice().longValue());
    }
}
