package org.eternity.food.shop.query.controller;

import org.eternity.food.base.web.PagedResponse;
import org.eternity.food.shop.query.persistence.ShopResponses;
import org.eternity.food.shop.query.service.ShopQueryService;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/shops")
public class ShopQueryController {
    private final ShopQueryService shopQueryService;

    public ShopQueryController(ShopQueryService shopQueryService) {
        this.shopQueryService = shopQueryService;
    }

    @GetMapping
    public PagedResponse<ShopResponses.Nearby> list(
            @RequestParam double lat,
            @RequestParam double lng,
            @RequestParam(required = false) String category,
            @PageableDefault(size = 8) Pageable pageable) {
        Slice<ShopResponses.Nearby> slice = shopQueryService.findNearby(lat, lng, category, pageable);
        return PagedResponse.from(slice, pageable);
    }

    @GetMapping("/{shopId}")
    public ShopResponses.Detail get(@PathVariable Long shopId) {
        return shopQueryService.findById(shopId);
    }
}
