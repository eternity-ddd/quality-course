package org.eternity.food.dto;

/**
 * 가게 응답 DTOs. 절차지향 스타일이라 한 파일에 다 몰아넣음.
 */
public final class ShopDto {

    private ShopDto() {}

    public record Nearby(
            Long id,
            String name,
            String category,
            Double latitude,
            Double longitude,
            long minOrderAmount,
            long deliveryFee,
            double rating,
            String imageUrl,
            double distance,
            boolean open
    ) {}

    public record Detail(
            Long id,
            String name,
            String category,
            Double latitude,
            Double longitude,
            long minOrderAmount,
            long deliveryFee,
            double rating,
            String imageUrl,
            boolean open
    ) {}
}
