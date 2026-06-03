package org.eternity.food.order.query.controller;

import org.eternity.food.base.web.PagedResponse;
import org.eternity.food.order.query.persistence.OrderResponses;
import org.eternity.food.order.query.service.OrderQueryService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/orders")
public class OrderQueryController {
    private static final Long DEMO_USER_ID = 1L;

    private final OrderQueryService orderQueryService;

    public OrderQueryController(OrderQueryService orderQueryService) {
        this.orderQueryService = orderQueryService;
    }

    @GetMapping
    public PagedResponse<OrderResponses.Item> list(
            @PageableDefault(size = 10, sort = {"orderedTime", "id"}, direction = Sort.Direction.DESC) Pageable pageable) {
        Page<OrderResponses.Item> page = orderQueryService.findOrders(DEMO_USER_ID, pageable);
        return PagedResponse.from(page, pageable);
    }
}
