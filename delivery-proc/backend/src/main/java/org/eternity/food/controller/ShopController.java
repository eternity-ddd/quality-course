package org.eternity.food.controller;

import org.eternity.food.dto.ShopDto;
import org.eternity.food.service.ShopService;
import org.eternity.food.web.PagedResponse;
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
public class ShopController {

    private final ShopService shopService;

    public ShopController(ShopService shopService) {
        this.shopService = shopService;
    }

    @GetMapping
    public PagedResponse<ShopDto.Nearby> list(
            @RequestParam double lat,
            @RequestParam double lng,
            @RequestParam(required = false) String category,
            @PageableDefault(size = 8) Pageable pageable) {
        Slice<ShopDto.Nearby> slice = shopService.findNearby(lat, lng, category, pageable);
        return PagedResponse.from(slice, pageable);
    }

    @GetMapping("/{shopId}")
    public ShopDto.Detail get(@PathVariable Long shopId) {
        return shopService.findById(shopId);
    }
}
