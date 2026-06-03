package org.eternity.food.shop.query.persistence;

import org.eternity.food.base.generic.time.TimePeriod;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.Time;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@Repository
public class ShopQueryDao {
    private final JdbcTemplate jdbc;

    public ShopQueryDao(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * 위/경도 컬럼만으로 두 점 사이 거리(km)를 구하는 Haversine 식.
     * MySQL 공간함수(ST_Distance_Sphere) 대신 표준 수학함수만 사용하므로 H2에서도 동작한다.
     * lat/lng는 컨트롤러에서 double로 파싱된 값이라 SQL 인라인이 안전하다.
     */
    private static String haversineKm(double lat, double lng) {
        return String.format(java.util.Locale.US,
                "(6371 * ACOS(LEAST(1.0, "
                        + "COS(RADIANS(%1$s)) * COS(RADIANS(s.latitude)) "
                        + "* COS(RADIANS(s.longitude) - RADIANS(%2$s)) "
                        + "+ SIN(RADIANS(%1$s)) * SIN(RADIANS(s.latitude)))))",
                lat, lng);
    }

    private static final RowMapper<ShopResponses.Nearby> NEARBY_MAPPER = (rs, i) -> new ShopResponses.Nearby(
            rs.getLong("id"),
            rs.getString("name"),
            ShopResponses.CategoryNames.toDisplay(rs.getString("category")),
            rs.getDouble("latitude"),
            rs.getDouble("longitude"),
            rs.getLong("min_order_price"),
            0L,
            4.5,
            null,
            rs.getDouble("distance_km"),
            isOpenNow(rs.getObject("start_time", Time.class), rs.getObject("end_time", Time.class))
    );

    public Slice<ShopResponses.Nearby> findNearby(double lat, double lng, String category, Pageable pageable) {
        int limit = pageable.getPageSize() + 1;
        long offset = pageable.getOffset();

        String distance = haversineKm(lat, lng);
        String cols = "s.id, s.name, s.category, s.latitude, s.longitude, s.min_order_price, "
                + "ROUND(" + distance + ", 1) AS distance_km, s.start_time, s.end_time";

        String sql = "SELECT " + cols + " FROM shop s WHERE "
                + distance + " <= s.delivery_radius "
                + (category != null ? "AND s.category = ? " : "")
                + "ORDER BY distance_km ASC, s.id ASC LIMIT ? OFFSET ?";

        List<ShopResponses.Nearby> rows = category != null
                ? jdbc.query(sql, NEARBY_MAPPER, category, limit, offset)
                : jdbc.query(sql, NEARBY_MAPPER, limit, offset);

        boolean hasNext = rows.size() > pageable.getPageSize();
        if (hasNext) {
            rows = rows.subList(0, pageable.getPageSize());
        }
        return new SliceImpl<>(rows, pageable, hasNext);
    }

    public Optional<ShopResponses.Detail> findById(Long shopId) {
        String sql = """
                SELECT id, name, category, latitude, longitude, min_order_price, start_time, end_time
                FROM shop WHERE id = ?
                """;
        List<ShopResponses.Detail> rows = jdbc.query(sql, (rs, i) -> new ShopResponses.Detail(
                rs.getLong("id"),
                rs.getString("name"),
                ShopResponses.CategoryNames.toDisplay(rs.getString("category")),
                (Double) rs.getObject("latitude"),
                (Double) rs.getObject("longitude"),
                rs.getLong("min_order_price"),
                0L,
                4.5,
                null,
                isOpenNow(rs.getObject("start_time", Time.class), rs.getObject("end_time", Time.class))
        ), shopId);
        return rows.stream().findFirst();
    }

    public Optional<ShopResponses.Brief> findBriefById(Long shopId) {
        String sql = "SELECT id, name, min_order_price, start_time, end_time FROM shop WHERE id = ?";
        List<ShopResponses.Brief> rows = jdbc.query(sql, (rs, i) -> new ShopResponses.Brief(
                rs.getLong("id"),
                rs.getString("name"),
                rs.getLong("min_order_price"),
                isOpenNow(rs.getObject("start_time", Time.class), rs.getObject("end_time", Time.class))
        ), shopId);
        return rows.stream().findFirst();
    }

    private static boolean isOpenNow(Time start, Time end) {
        if (start == null || end == null) {
            return false;
        }
        return TimePeriod.between(start.toLocalTime(), end.toLocalTime())
                .contains(LocalTime.now());
    }
}
