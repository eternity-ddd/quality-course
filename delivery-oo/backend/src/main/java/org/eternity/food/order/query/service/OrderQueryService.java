package org.eternity.food.order.query.service;

import org.eternity.food.order.query.persistence.OrderQueryDao;
import org.eternity.food.order.query.persistence.OrderResponses;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderQueryService {
    private final OrderQueryDao orderQueryDao;

    public OrderQueryService(OrderQueryDao orderQueryDao) {
        this.orderQueryDao = orderQueryDao;
    }

    @Transactional(readOnly = true)
    public Page<OrderResponses.Item> findOrders(Long userId, Pageable pageable) {
        return orderQueryDao.findByUserId(userId, pageable);
    }
}
