package org.eternity.food.service;

import org.eternity.food.dto.ShopDto;
import org.eternity.food.entity.Shop;
import org.eternity.food.repository.ShopRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.Time;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 가게 관련 fat-service. 조회/매핑/검증 다 여기서.
 *
 * <p>레이아웃 anti-pattern: native query → Object[] → DTO 매핑이 service 안에서 직접 일어남.
 */
@Service
public class ShopService {

    private final ShopRepository shopRepository;

    public ShopService(ShopRepository shopRepository) {
        this.shopRepository = shopRepository;
    }

    // ====================================================================
    // 가게 생성 + 구조 검증
    // ====================================================================

    /**
     * 가게 등록. 구조 검증 후 저장.
     *
     * @return 저장된 Shop entity (id 채워짐)
     */
    @Transactional
    public Shop create(String name,
                       Long minOrderPrice,
                       LocalTime startTime,
                       LocalTime endTime,
                       String category,
                       Double latitude,
                       Double longitude,
                       Double deliveryRadius) {
        validateShop(name, minOrderPrice, startTime, endTime);
        validateLocation(latitude, longitude);

        Shop shop = new Shop();
        shop.setName(name);
        shop.setMinOrderPrice(minOrderPrice);
        shop.setStartTime(startTime);
        shop.setEndTime(endTime);
        shop.setCategory(category);
        shop.setLatitude(latitude);
        shop.setLongitude(longitude);
        shop.setDeliveryRadius(deliveryRadius);

        return shopRepository.save(shop);
    }

    /**
     * Shop 구조 검증.
     * <ul>
     *   <li>name != null</li>
     *   <li>name.length >= 5</li>
     *   <li>minOrderPrice != null</li>
     *   <li>minOrderPrice > 0</li>
     *   <li>operationPeriod != null — startTime/endTime null 아님 + start &lt; end</li>
     * </ul>
     */
    void validateShop(String name, Long minOrderPrice, LocalTime startTime, LocalTime endTime) {
        if (name == null || name.length() < 5) {
            throw new IllegalArgumentException("가게 이름은 5글자 이상이어야 합니다.");
        }

        if (minOrderPrice == null || minOrderPrice <= 0L) {
            throw new IllegalArgumentException("최소주문금액은 0원보다 커야 합니다.");
        }

        if (startTime == null || endTime == null) {
            throw new IllegalArgumentException("영업시간은 null이어서는 안됩니다.");
        }

        if (!startTime.isBefore(endTime)) {
            throw new IllegalArgumentException("영업 시작 시각은 종료 시각보다 빨라야 합니다.");
        }
    }

    /**
     * 좌표 검증.
     * <ul>
     *   <li>latitude, longitude != null</li>
     *   <li>latitude ∈ [-90, 90]</li>
     *   <li>longitude ∈ [-180, 180]</li>
     * </ul>
     */
    void validateLocation(Double latitude, Double longitude) {
        if (latitude == null || longitude == null) {
            throw new IllegalArgumentException("좌표는 null이어서는 안됩니다.");
        }
        if (latitude < -90 || latitude > 90) {
            throw new IllegalArgumentException("위도는 -90~90 범위여야 합니다: " + latitude);
        }
        if (longitude < -180 || longitude > 180) {
            throw new IllegalArgumentException("경도는 -180~180 범위여야 합니다: " + longitude);
        }
    }

    @Transactional(readOnly = true)
    public Slice<ShopDto.Nearby> findNearby(double lat, double lng, String category, Pageable pageable) {
        // 1. 카테고리 표시명 → DB 코드
        String categoryCode = null;
        if (category != null && !category.isBlank() && !"전체".equals(category)) {
            categoryCode = CategoryNames.toCode(category);
        }

        // 2. native 쿼리 호출 (Haversine 거리 계산). Object[] 로 row 받음.
        Slice<Object[]> rows = shopRepository.findNearbyWithDistance(lat, lng, categoryCode, pageable);

        // 3. 손으로 한 줄씩 DTO 매핑. 컬럼 순서 schema.sql 기준이라 깨지면 여기서 터짐.
        // shop 컬럼: id, name, min_order_price, start_time, end_time, category, latitude, longitude, delivery_radius
        // 그리고 마지막에 distance_km
        List<ShopDto.Nearby> content = new ArrayList<>();
        for (Object[] row : rows.getContent()) {
            Long id = ((Number) row[0]).longValue();
            String name = (String) row[1];
            Long minOrderPrice = ((Number) row[2]).longValue();
            Time startTime = (Time) row[3];
            Time endTime = (Time) row[4];
            String categoryCodeOut = (String) row[5];
            Double latitude = row[6] == null ? null : ((Number) row[6]).doubleValue();
            Double longitude = row[7] == null ? null : ((Number) row[7]).doubleValue();
            // row[8] = delivery_radius - 사용 안 함
            double distance = ((Number) row[row.length - 1]).doubleValue();

            content.add(new ShopDto.Nearby(
                    id,
                    name,
                    CategoryNames.toDisplay(categoryCodeOut),
                    latitude,
                    longitude,
                    minOrderPrice,
                    0L,
                    4.5,
                    null,
                    distance,
                    isOpenNow(startTime, endTime)
            ));
        }

        return new SliceImpl<>(content, pageable, rows.hasNext());
    }

    @Transactional(readOnly = true)
    public ShopDto.Detail findById(Long shopId) {
        Shop shop = shopRepository.findById(shopId)
                .orElseThrow(() -> new IllegalArgumentException("가게를 찾을 수 없습니다: " + shopId));

        return new ShopDto.Detail(
                shop.getId(),
                shop.getName(),
                CategoryNames.toDisplay(shop.getCategory()),
                shop.getLatitude(),
                shop.getLongitude(),
                shop.getMinOrderPrice(),
                0L,
                4.5,
                null,
                isOpenNow(toTime(shop.getStartTime()), toTime(shop.getEndTime()))
        );
    }

    /**
     * 카트 조회 / 주문 등에서 가게 한 줄 정보가 필요할 때 쓰는 헬퍼.
     */
    @Transactional(readOnly = true)
    public Shop loadShopOrThrow(Long shopId) {
        return shopRepository.findById(shopId)
                .orElseThrow(() -> new IllegalArgumentException("가게를 찾을 수 없습니다: " + shopId));
    }

    public boolean isShopOpen(Shop shop) {
        return isOpenNow(toTime(shop.getStartTime()), toTime(shop.getEndTime()));
    }

    private Time toTime(LocalTime t) {
        return t == null ? null : Time.valueOf(t);
    }

    private static boolean isOpenNow(Time start, Time end) {
        if (start == null || end == null) {
            return false;
        }
        LocalTime s = start.toLocalTime();
        LocalTime e = end.toLocalTime();
        LocalTime now = LocalTime.now();

        if (s.equals(e) || s.isAfter(e)) {
            // 자정 넘는 케이스나 동일한 케이스는 그냥 false 처리 (간단히)
            return false;
        }

        boolean afterOrEqualStart = !now.isBefore(s);
        boolean beforeOrEqualEnd = !now.isAfter(e);
        return afterOrEqualStart && beforeOrEqualEnd;
    }

    // 호환용 (안 쓰지만 향후 누군가 호출할 수도 있어서 남겨둠 - 절차지향 코드에서 흔한 dead code)
    @SuppressWarnings("unused")
    private static double round1(double v) {
        return new BigDecimal(v).setScale(1, java.math.RoundingMode.HALF_UP).doubleValue();
    }
}
