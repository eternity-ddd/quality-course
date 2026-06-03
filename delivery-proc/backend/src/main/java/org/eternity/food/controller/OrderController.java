package org.eternity.food.controller;

import org.eternity.food.dto.OrderDto;
import org.eternity.food.service.OrderService;
import org.eternity.food.web.PagedResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private static final Long DEMO_USER_ID = 1L;

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping
    public PagedResponse<OrderDto.OrderResponse> list(
            @PageableDefault(size = 10, sort = {"orderedTime", "id"}, direction = Sort.Direction.DESC) Pageable pageable) {
        Page<OrderDto.OrderResponse> page = orderService.findOrders(DEMO_USER_ID, pageable);
        return PagedResponse.from(page, pageable);
    }
}
