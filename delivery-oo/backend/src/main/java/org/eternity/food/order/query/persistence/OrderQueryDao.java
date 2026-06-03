package org.eternity.food.order.query.persistence;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eternity.food.order.query.persistence.OrderResponses.Item;
import org.eternity.food.order.query.persistence.OrderResponses.Item.LineItem;
import org.eternity.food.order.query.persistence.OrderResponses.Item.Option;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.util.List;
import java.util.Map;

@Repository
public class OrderQueryDao {
    private final JdbcTemplate jdbc;
    private final ObjectMapper objectMapper;

    public OrderQueryDao(JdbcTemplate jdbc, ObjectMapper objectMapper) {
        this.jdbc = jdbc;
        this.objectMapper = objectMapper;
    }

    public Page<Item> findByUserId(Long userId, Pageable pageable) {
        Long total = jdbc.queryForObject(
                "SELECT COUNT(*) FROM orders WHERE user_id = ?", Long.class, userId);
        long totalElements = total == null ? 0L : total;

        if (totalElements == 0) {
            return new PageImpl<>(List.of(), pageable, 0);
        }

        String orderBy = pageable.getSort().isEmpty()
                ? "o.ordered_time DESC, o.id DESC"
                : pageable.getSort().stream()
                    .map(o -> mapSortField(o.getProperty()) + (o.getDirection() == Sort.Direction.ASC ? " ASC" : " DESC"))
                    .reduce((a, b) -> a + ", " + b)
                    .orElse("o.ordered_time DESC, o.id DESC");

        String sql = """
                SELECT o.id, o.shop_id, s.name AS shop_name,
                       o.ordered_time, o.total_price, o.items_snapshot
                FROM orders o
                JOIN shop s ON s.id = o.shop_id
                WHERE o.user_id = ?
                ORDER BY\s\
                """ + orderBy + " LIMIT ? OFFSET ?";

        List<Item> content = jdbc.query(sql, (rs, i) -> {
            String json = rs.getString("items_snapshot");
            List<LineItem> items = parseItems(json);
            return new Item(
                    rs.getLong("id"),
                    rs.getLong("shop_id"),
                    rs.getString("shop_name"),
                    ((Timestamp) rs.getObject("ordered_time")).toLocalDateTime(),
                    rs.getLong("total_price"),
                    items
            );
        }, userId, pageable.getPageSize(), pageable.getOffset());

        return new PageImpl<>(content, pageable, totalElements);
    }

    private String mapSortField(String field) {
        return switch (field) {
            case "orderedTime" -> "o.ordered_time";
            case "totalPrice" -> "o.total_price";
            case "shopId" -> "o.shop_id";
            case "id" -> "o.id";
            default -> "o." + field;
        };
    }

    @SuppressWarnings("unchecked")
    private List<LineItem> parseItems(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            List<Map<String, Object>> raw = objectMapper.readValue(json, new TypeReference<>() {});
            return raw.stream().map(this::toItem).toList();
        } catch (Exception e) {
            throw new IllegalStateException("주문 스냅샷 파싱 실패", e);
        }
    }

    @SuppressWarnings("unchecked")
    private LineItem toItem(Map<String, Object> m) {
        String menuName = (String) m.get("menuName");
        int count = ((Number) m.getOrDefault("count", 0)).intValue();
        long unitPrice = ((Number) m.getOrDefault("unitPrice", 0)).longValue();
        long subtotal = unitPrice * count;

        List<Map<String, Object>> groups = (List<Map<String, Object>>) m.getOrDefault("groups", List.of());
        List<Option> options = groups.stream()
                .flatMap(g -> {
                    String groupName = (String) g.get("name");
                    List<Map<String, Object>> opts = (List<Map<String, Object>>) g.getOrDefault("options", List.of());
                    return opts.stream().map(o -> new Option(
                            groupName,
                            (String) o.get("name"),
                            ((Number) o.getOrDefault("price", 0)).longValue()
                    ));
                })
                .toList();
        return new LineItem(menuName, count, unitPrice, subtotal, options);
    }
}
