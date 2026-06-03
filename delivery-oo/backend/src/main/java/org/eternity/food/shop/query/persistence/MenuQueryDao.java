package org.eternity.food.shop.query.persistence;

import org.eternity.food.shop.query.persistence.MenuResponses.Detail;
import org.eternity.food.shop.query.persistence.MenuResponses.Detail.Option;
import org.eternity.food.shop.query.persistence.MenuResponses.Detail.OptionGroup;
import org.eternity.food.shop.query.persistence.MenuResponses.Item;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public class MenuQueryDao {
    private final JdbcTemplate jdbc;

    public MenuQueryDao(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Item> findByShopId(Long shopId) {
        String sql = "SELECT id, name, description, base_price FROM menu WHERE shop_id = ? ORDER BY id";
        return jdbc.query(sql, (rs, i) -> new Item(
                rs.getLong("id"),
                rs.getString("name"),
                rs.getString("description"),
                rs.getLong("base_price"),
                null
        ), shopId);
    }

    public Optional<Detail> findDetail(Long menuId) {
        String menuSql = "SELECT id, name, description, base_price FROM menu WHERE id = ?";
        List<Item> menus = jdbc.query(menuSql, (rs, i) -> new Item(
                rs.getLong("id"),
                rs.getString("name"),
                rs.getString("description"),
                rs.getLong("base_price"),
                null
        ), menuId);

        if (menus.isEmpty()) {
            return Optional.empty();
        }
        Item m = menus.get(0);

        String groupSql = """
                SELECT og.id AS og_id, og.name AS og_name, og.required AS og_required,
                       mog.display_order AS display_order,
                       o.id AS opt_id, o.name AS opt_name, o.price AS opt_price
                FROM menu_option_group mog
                JOIN option_group og ON og.id = mog.option_group_id
                LEFT JOIN MENU_OPTION o ON o.option_group_id = og.id
                WHERE mog.menu_id = ?
                ORDER BY mog.display_order ASC, o.price ASC, o.name ASC
                """;

        Map<Long, GroupAccumulator> groups = new HashMap<>();
        List<Long> order = new ArrayList<>();

        jdbc.query(groupSql, rs -> {
            long ogId = rs.getLong("og_id");
            GroupAccumulator acc = groups.get(ogId);
            if (acc == null) {
                acc = new GroupAccumulator(
                        ogId,
                        rs.getString("og_name"),
                        rs.getBoolean("og_required"),
                        new ArrayList<>());
                groups.put(ogId, acc);
                order.add(ogId);
            }
            long optId = rs.getLong("opt_id");
            if (!rs.wasNull()) {
                acc.options.add(new Option(optId, rs.getString("opt_name"), rs.getLong("opt_price")));
            }
        }, menuId);

        List<OptionGroup> optionGroups = order.stream()
                .map(groups::get)
                .map(a -> new OptionGroup(a.id, a.name, a.required, a.options))
                .toList();

        return Optional.of(new Detail(
                m.id(), m.name(), m.description(), m.price(), m.imageUrl(), optionGroups));
    }

    private record GroupAccumulator(
            Long id, String name, boolean required, List<Option> options
    ) {}
}
