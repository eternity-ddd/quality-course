package org.eternity.food.cart.query.persistence;

import org.eternity.food.cart.query.persistence.CartRaw.CartItemRaw;
import org.eternity.food.cart.query.persistence.CartRaw.CartOptionGroupRaw;
import org.eternity.food.cart.query.persistence.CartRaw.CartOptionRaw;
import org.eternity.food.cart.query.persistence.CatalogSnapshot.MenuInfo;
import org.eternity.food.cart.query.persistence.CatalogSnapshot.OptionGroupInfo;
import org.eternity.food.cart.query.persistence.CatalogSnapshot.OptionInfo;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Repository
public class CartQueryDao {
    private final JdbcTemplate jdbc;
    private final NamedParameterJdbcTemplate namedJdbc;

    public CartQueryDao(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
        this.namedJdbc = new NamedParameterJdbcTemplate(jdbc);
    }

    public Optional<CartRaw> findRawByUserId(Long userId) {
        String cartSql = "SELECT id, user_id, shop_id FROM cart WHERE user_id = ?";
        List<CartHeader> headers = jdbc.query(cartSql, (rs, i) -> new CartHeader(
                rs.getLong("id"),
                rs.getLong("user_id"),
                (Long) rs.getObject("shop_id")
        ), userId);

        if (headers.isEmpty()) {
            return Optional.empty();
        }
        CartHeader header = headers.get(0);

        String itemSql = """
                SELECT cli.id AS cli_id, cli.menu_id, cli.menu_name, cli.menu_count, cli.base_price,
                       cog.id AS cog_id, cog.option_group_id AS cog_option_group_id, cog.name AS cog_name,
                       co.name AS co_name, co.price AS co_price
                FROM cart_line_item cli
                LEFT JOIN cart_option_group cog ON cog.cart_line_item_id = cli.id
                LEFT JOIN cart_option co ON co.cart_option_group_id = cog.id
                WHERE cli.cart_id = ?
                ORDER BY cli.id ASC, cog.id ASC, co.name ASC
                """;

        Map<Long, ItemAccumulator> itemsById = new LinkedHashMap<>();

        jdbc.query(itemSql, rs -> {
            long itemId = rs.getLong("cli_id");
            ItemAccumulator acc = itemsById.get(itemId);
            if (acc == null) {
                acc = new ItemAccumulator(
                        itemId,
                        rs.getLong("menu_id"),
                        rs.getString("menu_name"),
                        rs.getInt("menu_count"),
                        rs.getLong("base_price"),
                        new LinkedHashMap<>()
                );
                itemsById.put(itemId, acc);
            }

            long cogId = rs.getLong("cog_id");
            if (rs.wasNull()) {
                return;
            }

            GroupAccumulator gAcc = acc.groupsById.get(cogId);
            if (gAcc == null) {
                gAcc = new GroupAccumulator(
                        rs.getLong("cog_option_group_id"),
                        rs.getString("cog_name"),
                        new ArrayList<>()
                );
                acc.groupsById.put(cogId, gAcc);
            }

            String optName = rs.getString("co_name");
            if (optName != null) {
                gAcc.options.add(new CartOptionRaw(
                        optName,
                        rs.getLong("co_price")
                ));
            }
        }, header.id);

        List<CartItemRaw> items = itemsById.values().stream()
                .map(a -> new CartItemRaw(
                        a.id,
                        a.menuId,
                        a.menuName,
                        a.quantity,
                        a.basePrice,
                        a.groupsById.values().stream()
                                .map(g -> new CartOptionGroupRaw(g.optionGroupId, g.name, g.options))
                                .toList()
                ))
                .toList();

        return Optional.of(new CartRaw(header.id, header.userId, header.shopId, items));
    }

    public CatalogSnapshot loadCatalogFor(Set<Long> menuIds) {
        if (menuIds.isEmpty()) {
            return new CatalogSnapshot(Map.of(), Map.of());
        }

        Map<Long, MenuInfo> menus = loadMenus(menuIds);
        Set<Long> optionGroupIds = menus.values().stream()
                .flatMap(m -> m.optionGroupIds().stream())
                .collect(Collectors.toSet());
        Map<Long, OptionGroupInfo> optionGroups = loadOptionGroups(optionGroupIds);

        return new CatalogSnapshot(menus, optionGroups);
    }

    private Map<Long, MenuInfo> loadMenus(Set<Long> menuIds) {
        String sql = """
                SELECT m.id, m.name, m.status, m.base_price, mog.option_group_id
                FROM menu m
                LEFT JOIN menu_option_group mog ON mog.menu_id = m.id
                WHERE m.id IN (:ids)
                ORDER BY m.id, mog.display_order
                """;

        MapSqlParameterSource params = new MapSqlParameterSource("ids", menuIds);
        Map<Long, MenuAccumulator> acc = new LinkedHashMap<>();

        namedJdbc.query(sql, params, rs -> {
            long id = rs.getLong("id");
            MenuAccumulator m = acc.get(id);
            if (m == null) {
                m = new MenuAccumulator(id, rs.getString("name"), rs.getString("status"), rs.getLong("base_price"), new ArrayList<>());
                acc.put(id, m);
            }
            long ogId = rs.getLong("option_group_id");
            if (!rs.wasNull()) {
                m.optionGroupIds.add(ogId);
            }
        });

        return acc.values().stream()
                .collect(Collectors.toMap(
                        m -> m.id,
                        m -> new MenuInfo(m.id, m.name, m.status, m.basePrice, m.optionGroupIds)
                ));
    }

    private Map<Long, OptionGroupInfo> loadOptionGroups(Set<Long> optionGroupIds) {
        if (optionGroupIds.isEmpty()) {
            return Map.of();
        }

        String sql = """
                SELECT og.id, og.name, o.id AS option_id, o.name AS option_name, o.price AS option_price
                FROM option_group og
                LEFT JOIN MENU_OPTION o ON o.option_group_id = og.id
                WHERE og.id IN (:ids)
                ORDER BY og.id, o.id
                """;

        MapSqlParameterSource params = new MapSqlParameterSource("ids", optionGroupIds);
        Map<Long, OptionGroupAccumulatorumulator> acc = new LinkedHashMap<>();

        namedJdbc.query(sql, params, rs -> {
            long id = rs.getLong("id");
            OptionGroupAccumulatorumulator og = acc.get(id);
            if (og == null) {
                og = new OptionGroupAccumulatorumulator(id, rs.getString("name"), new LinkedHashMap<>());
                acc.put(id, og);
            }
            String optionName = rs.getString("option_name");
            if (optionName != null) {
                og.optionsByName.put(optionName, new OptionInfo(
                        optionName,
                        rs.getLong("option_price")
                ));
            }
        });

        return acc.values().stream()
                .collect(Collectors.toMap(
                        og -> og.id,
                        og -> new OptionGroupInfo(og.id, og.name, Collections.unmodifiableMap(og.optionsByName))
                ));
    }

    private record CartHeader(Long id, Long userId, Long shopId) {}

    private static final class ItemAccumulator {
        final Long id;
        final Long menuId;
        final String menuName;
        final int quantity;
        final long basePrice;
        final Map<Long, GroupAccumulator> groupsById;

        ItemAccumulator(Long id, Long menuId, String menuName, int quantity, long basePrice, Map<Long, GroupAccumulator> groupsById) {
            this.id = id;
            this.menuId = menuId;
            this.menuName = menuName;
            this.quantity = quantity;
            this.basePrice = basePrice;
            this.groupsById = groupsById;
        }
    }

    private static final class GroupAccumulator {
        final Long optionGroupId;
        final String name;
        final List<CartOptionRaw> options;

        GroupAccumulator(Long optionGroupId, String name, List<CartOptionRaw> options) {
            this.optionGroupId = optionGroupId;
            this.name = name;
            this.options = options;
        }
    }

    private static final class MenuAccumulator {
        final long id;
        final String name;
        final String status;
        final long basePrice;
        final List<Long> optionGroupIds;

        MenuAccumulator(long id, String name, String status, long basePrice, List<Long> optionGroupIds) {
            this.id = id;
            this.name = name;
            this.status = status;
            this.basePrice = basePrice;
            this.optionGroupIds = optionGroupIds;
        }
    }

    private static final class OptionGroupAccumulatorumulator {
        final long id;
        final String name;
        final Map<String, OptionInfo> optionsByName;

        OptionGroupAccumulatorumulator(long id, String name, Map<String, OptionInfo> optionsByName) {
            this.id = id;
            this.name = name;
            this.optionsByName = optionsByName;
        }
    }
}
