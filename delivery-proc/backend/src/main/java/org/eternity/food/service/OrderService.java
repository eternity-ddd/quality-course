package org.eternity.food.service;

import org.eternity.food.dto.OrderDto;
import org.eternity.food.entity.Order;
import org.eternity.food.entity.OrderLineItem;
import org.eternity.food.entity.Shop;
import org.eternity.food.repository.OrderRepository;
import org.eternity.food.repository.ShopRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 주문 조회 fat-service. JSON snapshot → DTO 직접 풀어내기.
 */
@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final ShopRepository shopRepository;

    public OrderService(OrderRepository orderRepository,
                        ShopRepository shopRepository) {
        this.orderRepository = orderRepository;
        this.shopRepository = shopRepository;
    }

    @Transactional(readOnly = true)
    public Page<OrderDto.OrderResponse> findOrders(Long userId, Pageable pageable) {
        Page<Order> page = orderRepository.findByUserId(userId, pageable);
        if (page.isEmpty()) {
            return new PageImpl<>(List.of(), pageable, page.getTotalElements());
        }

        // shopId 모아서 한 번에 가게 이름 조회
        Set<Long> shopIds = new HashSet<>();
        for (Order o : page.getContent()) {
            shopIds.add(o.getShopId());
        }
        Map<Long, String> shopNameById = new HashMap<>();
        for (Shop s : shopRepository.findAllById(shopIds)) {
            shopNameById.put(s.getId(), s.getName());
        }

        List<OrderDto.OrderResponse> content = new ArrayList<>();
        for (Order o : page.getContent()) {
            List<OrderDto.LineItem> lines = new ArrayList<>();
            List<OrderLineItem> snaps = o.getItemsSnapshot();
            if (snaps != null) {
                for (OrderLineItem snap : snaps) {
                    List<OrderDto.Option> options = new ArrayList<>();
                    if (snap.getGroups() != null) {
                        for (OrderLineItem.OrderOptionGroup g : snap.getGroups()) {
                            if (g.getOptions() == null) {
                                continue;
                            }
                            for (OrderLineItem.OrderOption opt : g.getOptions()) {
                                options.add(new OrderDto.Option(
                                        g.getName(),
                                        opt.getName(),
                                        opt.getPrice() == null ? 0L : opt.getPrice()
                                ));
                            }
                        }
                    }

                    int count = snap.getCount() == null ? 0 : snap.getCount();
                    long unitPrice = snap.getUnitPrice() == null ? 0L : snap.getUnitPrice();
                    lines.add(new OrderDto.LineItem(
                            snap.getMenuName(),
                            count,
                            unitPrice,
                            unitPrice * count,
                            options
                    ));
                }
            }

            content.add(new OrderDto.OrderResponse(
                    o.getId(),
                    o.getShopId(),
                    shopNameById.get(o.getShopId()),
                    o.getOrderedTime(),
                    o.getTotalPrice() == null ? 0L : o.getTotalPrice(),
                    lines
            ));
        }

        return new PageImpl<>(content, pageable, page.getTotalElements());
    }
}
