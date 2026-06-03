package org.eternity.food.repository;

import org.eternity.food.entity.Shop;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ShopRepository extends JpaRepository<Shop, Long> {

    /**
     * 반경 내 가게 + 거리(km) 같이 가져오는 native 쿼리.
     * Object[] 로 row 받아서 service에서 직접 풀어준다.
     * - [0] Shop entity
     * - [1] distance_km (Double)
     */
    @Query(value = """
            SELECT s.*,
                   ROUND(6371 * ACOS(LEAST(1.0,
                       COS(RADIANS(:lat)) * COS(RADIANS(s.latitude)) * COS(RADIANS(s.longitude) - RADIANS(:lng))
                       + SIN(RADIANS(:lat)) * SIN(RADIANS(s.latitude)))), 1) AS distance_km
            FROM shop s
            WHERE 6371 * ACOS(LEAST(1.0,
                      COS(RADIANS(:lat)) * COS(RADIANS(s.latitude)) * COS(RADIANS(s.longitude) - RADIANS(:lng))
                      + SIN(RADIANS(:lat)) * SIN(RADIANS(s.latitude)))) <= s.delivery_radius
              AND (:category IS NULL OR s.category = :category)
            ORDER BY distance_km ASC, s.id ASC
            """,
            countQuery = """
            SELECT count(*) FROM shop s
            WHERE 6371 * ACOS(LEAST(1.0,
                      COS(RADIANS(:lat)) * COS(RADIANS(s.latitude)) * COS(RADIANS(s.longitude) - RADIANS(:lng))
                      + SIN(RADIANS(:lat)) * SIN(RADIANS(s.latitude)))) <= s.delivery_radius
              AND (:category IS NULL OR s.category = :category)
            """,
            nativeQuery = true)
    Slice<Object[]> findNearbyWithDistance(@Param("lat") double lat,
                                           @Param("lng") double lng,
                                           @Param("category") String category,
                                           Pageable pageable);
}
