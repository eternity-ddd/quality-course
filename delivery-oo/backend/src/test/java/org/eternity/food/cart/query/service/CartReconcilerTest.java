package org.eternity.food.cart.query.service;

import org.eternity.food.cart.query.persistence.CartRaw;
import org.eternity.food.cart.query.persistence.CartRaw.CartItemRaw;
import org.eternity.food.cart.query.persistence.CartRaw.CartOptionGroupRaw;
import org.eternity.food.cart.query.persistence.CartRaw.CartOptionRaw;
import org.eternity.food.cart.query.persistence.CartResponses;
import org.eternity.food.cart.query.persistence.CartResponses.Cart;
import org.eternity.food.cart.query.persistence.CartResponses.Cart.Item;
import org.eternity.food.cart.query.persistence.CartResponses.Cart.Option;
import org.eternity.food.cart.query.persistence.CartResponses.ItemStatus;
import org.eternity.food.cart.query.persistence.CartResponses.OptionStatus;
import org.eternity.food.cart.query.persistence.CatalogSnapshot;
import org.eternity.food.cart.query.persistence.CatalogSnapshot.MenuInfo;
import org.eternity.food.cart.query.persistence.CatalogSnapshot.OptionGroupInfo;
import org.eternity.food.cart.query.persistence.CatalogSnapshot.OptionInfo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * CartReconciler — Cart의 catalog 정합성 검증 + status 분류.
 *
 * <p>Status matrix:
 * <pre>
 *   ┌─────────────────┬───────────────────────────────────────┐
 *   │ status          │ trigger                               │
 *   ├─────────────────┼───────────────────────────────────────┤
 *   │ VALID           │ snapshot 일치                         │
 *   │ NAME_UPDATED    │ (name 기반 식별 전환으로 미사용)       │
 *   │ PRICE_CHANGED   │ 가격 변경                             │
 *   │ INVALID_OPTION  │ 옵션 식별 불가 / 그룹 사라짐          │
 *   │ MENU_NOT_OPEN   │ 메뉴 status != OPEN                  │
 *   │ MENU_REMOVED    │ 메뉴 자체가 catalog에서 사라짐        │
 *   └─────────────────┴───────────────────────────────────────┘
 * </pre>
 */
@DisplayName("CartReconciler 단위 테스트")
class CartReconcilerTest {

    private final CartReconciler reconciler = new CartReconciler();

    private static final Long MENU_ID = 10L;
    private static final Long OPTION_GROUP_ID = 100L;
    private static final Long OPTION_ID = 1000L;
    private static final Long ITEM_ID = 9L;

    // ─────────────── helpers ───────────────

    private CartRaw cartRaw(CartItemRaw... items) {
        return new CartRaw(1L, 1L, 1L, List.of(items));
    }

    private CartItemRaw itemRaw(int qty, long basePrice, CartOptionGroupRaw... groups) {
        return new CartItemRaw(ITEM_ID, MENU_ID, "삼겹살 1인세트", qty, basePrice, List.of(groups));
    }

    private CartOptionGroupRaw groupRaw(String name, CartOptionRaw... opts) {
        return new CartOptionGroupRaw(OPTION_GROUP_ID, name, List.of(opts));
    }

    private CartOptionRaw optionRaw(String name, long price) {
        return new CartOptionRaw(name, price);
    }

    private CatalogSnapshot snapshot(MenuInfo menu, OptionGroupInfo group) {
        return new CatalogSnapshot(
                menu == null ? Map.of() : Map.of(menu.id(), menu),
                group == null ? Map.of() : Map.of(group.id(), group)
        );
    }

    private MenuInfo menu(String name, String status, long basePrice) {
        return new MenuInfo(MENU_ID, name, status, basePrice, List.of(OPTION_GROUP_ID));
    }

    private OptionGroupInfo group(String name, OptionInfo... opts) {
        Map<String, OptionInfo> m = new java.util.HashMap<>();
        for (OptionInfo o : opts) m.put(o.name(), o);
        return new OptionGroupInfo(OPTION_GROUP_ID, name, m);
    }

    private OptionInfo option(String name, long price) {
        return new OptionInfo(name, price);
    }

    private Cart.Shop shop() {
        return new Cart.Shop(1L, "오겹돼지", 0L, 13_000L, true);
    }

    // ─────────────── VALID ───────────────

    @Nested
    @DisplayName("VALID — snapshot 일치")
    class Valid {

        @Test
        @DisplayName("메뉴/그룹/옵션 모두 일치 → 라인 status=VALID + 옵션 status=VALID + 메시지 없음")
        void allMatch_valid() {
            CartRaw raw = cartRaw(itemRaw(1, 10_000,
                    groupRaw("기본", optionRaw("소(250g)", 12_000))));
            CatalogSnapshot cat = snapshot(
                    menu("삼겹살 1인세트", "OPEN", 10_000),
                    group("기본", option("소(250g)", 12_000)));

            Cart result = reconciler.reconcile(raw, cat, shop());

            assertThat(result.items()).hasSize(1);
            Item item = result.items().get(0);
            assertThat(item.status()).isEqualTo(ItemStatus.VALID);
            assertThat(item.messages()).isEmpty();
            assertThat(item.basePrice()).isEqualTo(22_000L);
            assertThat(item.selectedOptions()).hasSize(1);
            assertThat(item.selectedOptions().get(0).status()).isEqualTo(OptionStatus.VALID);
        }

        @Test
        @DisplayName("totalPrice = basePrice × quantity 합산")
        void totalPrice_sums() {
            CartRaw raw = cartRaw(itemRaw(3, 10_000,
                    groupRaw("기본", optionRaw("소(250g)", 12_000))));
            CatalogSnapshot cat = snapshot(
                    menu("삼겹살 1인세트", "OPEN", 10_000),
                    group("기본", option("소(250g)", 12_000)));

            Cart result = reconciler.reconcile(raw, cat, shop());

            assertThat(result.totalPrice()).isEqualTo(22_000L * 3);
        }
    }

    // ─────────────── NAME_UPDATED ───────────────

    @Nested
    @DisplayName("옵션 이름 변경 — name 기반 식별이므로 INVALID 처리")
    class OptionNameChanged {

        @Test
        @DisplayName("옵션 이름 변경 → 기존 이름으로 찾을 수 없으므로 INVALID_OPTION")
        void optionNameChanged_invalid() {
            CartRaw raw = cartRaw(itemRaw(1, 10_000,
                    groupRaw("기본", optionRaw("소(250g)", 12_000))));
            CatalogSnapshot cat = snapshot(
                    menu("삼겹살 1인세트", "OPEN", 10_000),
                    group("기본", option("스몰(250g)", 12_000)));

            Cart result = reconciler.reconcile(raw, cat, shop());

            Item item = result.items().get(0);
            assertThat(item.status()).isEqualTo(ItemStatus.INVALID_OPTION);
            assertThat(item.selectedOptions().get(0).status()).isEqualTo(OptionStatus.INVALID);
        }

        @Test
        @DisplayName("Identity 유지 — line item의 menuId는 snapshot 기준 그대로")
        void identity_preserved() {
            CartRaw raw = cartRaw(itemRaw(1, 10_000,
                    groupRaw("기본", optionRaw("소(250g)", 12_000))));
            CatalogSnapshot cat = snapshot(
                    menu("삼겹살 1인세트", "OPEN", 10_000),
                    group("기본", option("소(250g)", 12_000)));

            Cart result = reconciler.reconcile(raw, cat, shop());

            Item item = result.items().get(0);
            assertThat(item.menuId()).isEqualTo(MENU_ID);
            assertThat(item.id()).isEqualTo(ITEM_ID);
        }
    }

    // ─────────────── 메뉴/그룹 이름 변경 안내 (message) ───────────────

    @Nested
    @DisplayName("메뉴/그룹 이름 변경 — message로 안내, status는 VALID")
    class NameChangeAsMessage {

        @Test
        @DisplayName("메뉴 이름 변경 → messages에 안내 + display는 최신 이름 + status VALID")
        void menuNameChanged_messageOnly() {
            CartRaw raw = cartRaw(itemRaw(1, 10_000,
                    groupRaw("기본", optionRaw("소(250g)", 12_000))));
            CatalogSnapshot cat = snapshot(
                    menu("프리미엄 삼겹살", "OPEN", 10_000),
                    group("기본", option("소(250g)", 12_000)));

            Cart result = reconciler.reconcile(raw, cat, shop());

            Item item = result.items().get(0);
            assertThat(item.status()).isEqualTo(ItemStatus.VALID);
            assertThat(item.menuName()).isEqualTo("프리미엄 삼겹살");
            assertThat(item.messages()).anyMatch(m -> m.contains("메뉴 이름이 변경"));
        }

        @Test
        @DisplayName("그룹 이름 변경 → messages에 안내 + display는 최신 이름 + status VALID")
        void groupNameChanged_messageOnly() {
            CartRaw raw = cartRaw(itemRaw(1, 10_000,
                    groupRaw("기본", optionRaw("소(250g)", 12_000))));
            CatalogSnapshot cat = snapshot(
                    menu("삼겹살 1인세트", "OPEN", 10_000),
                    group("사이즈", option("소(250g)", 12_000)));

            Cart result = reconciler.reconcile(raw, cat, shop());

            Item item = result.items().get(0);
            assertThat(item.status()).isEqualTo(ItemStatus.VALID);
            assertThat(item.selectedOptions().get(0).groupName()).isEqualTo("사이즈");
            assertThat(item.messages()).anyMatch(m -> m.contains("옵션 그룹 이름이 변경"));
        }
    }

    // ─────────────── PRICE_CHANGED ───────────────

    @Nested
    @DisplayName("PRICE_CHANGED — 가격 변경")
    class PriceChanged {

        @Test
        @DisplayName("옵션 가격 변경 → 옵션 status=PRICE_CHANGED + 라인 status=PRICE_CHANGED + 메시지")
        void optionPrice_changed() {
            CartRaw raw = cartRaw(itemRaw(1, 10_000,
                    groupRaw("기본", optionRaw("소(250g)", 12_000))));
            CatalogSnapshot cat = snapshot(
                    menu("삼겹살 1인세트", "OPEN", 10_000),
                    group("기본", option("소(250g)", 15_000)));

            Cart result = reconciler.reconcile(raw, cat, shop());

            Item item = result.items().get(0);
            assertThat(item.status()).isEqualTo(ItemStatus.PRICE_CHANGED);
            assertThat(item.selectedOptions().get(0).status())
                    .isEqualTo(OptionStatus.PRICE_CHANGED);
            assertThat(item.selectedOptions().get(0).price()).isEqualTo(15_000L);
            // 옵션 단위 PRICE_CHANGED는 라인 단위 message에도 안내
            assertThat(item.messages()).anyMatch(m -> m.contains("가격이 변경"));
        }

        @Test
        @DisplayName("메뉴 basePrice 변경(snapshot basePrice와 합산 결과가 다름) → 라인 status=PRICE_CHANGED")
        void menuBasePrice_changed() {
            // cart snapshot basePrice=22_000 (= base 10_000 + opt 12_000)
            // catalog basePrice=15_000 → unitTotal=27_000 → diff
            CartRaw raw = cartRaw(itemRaw(1, 10_000,
                    groupRaw("기본", optionRaw("소(250g)", 12_000))));
            CatalogSnapshot cat = snapshot(
                    menu("삼겹살 1인세트", "OPEN", 15_000),
                    group("기본", option("소(250g)", 12_000)));

            Cart result = reconciler.reconcile(raw, cat, shop());

            Item item = result.items().get(0);
            assertThat(item.status()).isEqualTo(ItemStatus.PRICE_CHANGED);
            assertThat(item.basePrice()).isEqualTo(27_000L);
        }
    }

    // ─────────────── INVALID_OPTION ───────────────

    @Nested
    @DisplayName("INVALID_OPTION — 옵션 식별 불가 / 그룹 사라짐")
    class InvalidOption {

        @Test
        @DisplayName("옵션이 catalog에서 사라짐 → 옵션 status=INVALID + 라인 status=INVALID_OPTION")
        void optionRemoved() {
            CartRaw raw = cartRaw(itemRaw(1, 10_000,
                    groupRaw("기본", optionRaw("소(250g)", 12_000))));
            // 그룹은 존재하지만 cart의 옵션 이름과 다른 옵션만 보유
            OptionInfo otherOpt = new OptionInfo("라지", 18_000);
            CatalogSnapshot cat = snapshot(
                    menu("삼겹살 1인세트", "OPEN", 10_000),
                    new OptionGroupInfo(OPTION_GROUP_ID, "기본", Map.of(otherOpt.name(), otherOpt)));

            Cart result = reconciler.reconcile(raw, cat, shop());

            Item item = result.items().get(0);
            assertThat(item.status()).isEqualTo(ItemStatus.INVALID_OPTION);
            assertThat(item.selectedOptions().get(0).status()).isEqualTo(OptionStatus.INVALID);
            assertThat(item.messages()).anyMatch(m -> m.contains("일부 옵션"));
        }

        @Test
        @DisplayName("그룹이 catalog에서 사라짐 → 모든 옵션 INVALID + 라인 INVALID_OPTION")
        void groupRemoved() {
            CartRaw raw = cartRaw(itemRaw(1, 10_000,
                    groupRaw("기본", optionRaw("소(250g)", 12_000))));
            CatalogSnapshot cat = snapshot(
                    menu("삼겹살 1인세트", "OPEN", 10_000),
                    null);

            Cart result = reconciler.reconcile(raw, cat, shop());

            Item item = result.items().get(0);
            assertThat(item.status()).isEqualTo(ItemStatus.INVALID_OPTION);
            assertThat(item.selectedOptions()).hasSize(1);
            assertThat(item.selectedOptions().get(0).status()).isEqualTo(OptionStatus.INVALID);
        }

        @Test
        @DisplayName("그룹이 메뉴의 optionGroupIds에 더 이상 포함되지 않음 → INVALID_OPTION")
        void groupNotInMenuConfig() {
            CartRaw raw = cartRaw(itemRaw(1, 10_000,
                    groupRaw("기본", optionRaw("소(250g)", 12_000))));

            // 메뉴는 다른 OPTION_GROUP_ID(=200L)만 참조
            MenuInfo m = new MenuInfo(MENU_ID, "삼겹살 1인세트", "OPEN", 10_000, List.of(200L));
            OptionGroupInfo g = group("기본", option("소(250g)", 12_000));
            CatalogSnapshot cat = new CatalogSnapshot(Map.of(m.id(), m), Map.of(g.id(), g));

            Cart result = reconciler.reconcile(raw, cat, shop());

            Item item = result.items().get(0);
            assertThat(item.status()).isEqualTo(ItemStatus.INVALID_OPTION);
            assertThat(item.selectedOptions().get(0).status()).isEqualTo(OptionStatus.INVALID);
        }
    }

    // ─────────────── MENU_NOT_OPEN ───────────────

    @Nested
    @DisplayName("MENU_NOT_OPEN — 메뉴 판매 중지")
    class MenuNotOpen {

        @Test
        @DisplayName("메뉴 status != OPEN → 라인 MENU_NOT_OPEN + 메시지 (옵션이 유효해도)")
        void menuClosed_overridesPrice() {
            // 옵션 가격까지 변경되어 있어도 MENU_NOT_OPEN이 우선순위
            CartRaw raw = cartRaw(itemRaw(1, 10_000,
                    groupRaw("기본", optionRaw("소(250g)", 12_000))));
            CatalogSnapshot cat = snapshot(
                    menu("삼겹살 1인세트", "READY", 10_000),
                    group("기본", option("소(250g)", 15_000)));

            Cart result = reconciler.reconcile(raw, cat, shop());

            Item item = result.items().get(0);
            assertThat(item.status()).isEqualTo(ItemStatus.MENU_NOT_OPEN);
            assertThat(item.messages()).anyMatch(m -> m.contains("판매중이 아닌"));
        }
    }

    // ─────────────── MENU_REMOVED ───────────────

    @Nested
    @DisplayName("MENU_REMOVED — 메뉴 catalog 자체에서 사라짐")
    class MenuRemoved {

        @Test
        @DisplayName("catalog menusById에 menuId 없음 → 라인 MENU_REMOVED + 모든 옵션 INVALID")
        void menuMissingFromCatalog() {
            CartRaw raw = cartRaw(itemRaw(1, 10_000,
                    groupRaw("기본", optionRaw("소(250g)", 12_000))));
            CatalogSnapshot cat = new CatalogSnapshot(Map.of(), Map.of());

            Cart result = reconciler.reconcile(raw, cat, shop());

            Item item = result.items().get(0);
            assertThat(item.status()).isEqualTo(ItemStatus.MENU_REMOVED);
            assertThat(item.selectedOptions()).hasSize(1);
            assertThat(item.selectedOptions().get(0).status()).isEqualTo(OptionStatus.INVALID);
            assertThat(item.messages()).anyMatch(m -> m.contains("더 이상 판매되지"));
        }

        @Test
        @DisplayName("MENU_REMOVED 라인은 snapshot의 menuName/basePrice 그대로 표시")
        void menuRemoved_preservesSnapshot() {
            CartRaw raw = cartRaw(itemRaw(1, 10_000,
                    groupRaw("기본", optionRaw("소(250g)", 12_000))));
            CatalogSnapshot cat = new CatalogSnapshot(Map.of(), Map.of());

            Cart result = reconciler.reconcile(raw, cat, shop());

            Item item = result.items().get(0);
            assertThat(item.menuName()).isEqualTo("삼겹살 1인세트");
            assertThat(item.basePrice()).isEqualTo(10_000L);
        }
    }

    // ─────────────── Shop 관련 ───────────────

    @Nested
    @DisplayName("Shop 정보 합성")
    class ShopComposition {

        @Test
        @DisplayName("reconcile 결과는 인자로 들어온 shop 정보를 그대로 carry")
        void shop_isCarriedAsIs() {
            CartRaw raw = cartRaw(itemRaw(1, 10_000,
                    groupRaw("기본", optionRaw("소(250g)", 12_000))));
            CatalogSnapshot cat = snapshot(
                    menu("삼겹살 1인세트", "OPEN", 10_000),
                    group("기본", option("소(250g)", 12_000)));
            Cart.Shop shop = new Cart.Shop(42L, "X가게", 3_000L, 20_000L, false);

            Cart result = reconciler.reconcile(raw, cat, shop);

            assertThat(result.shop()).isSameAs(shop);
            assertThat(result.shop().open()).isFalse();
        }

        @Test
        @DisplayName("shop=null도 그대로 들어감 (orphan cart)")
        void shop_nullCarried() {
            CartRaw raw = cartRaw(itemRaw(1, 10_000,
                    groupRaw("기본", optionRaw("소(250g)", 12_000))));
            CatalogSnapshot cat = snapshot(
                    menu("삼겹살 1인세트", "OPEN", 10_000),
                    group("기본", option("소(250g)", 12_000)));

            Cart result = reconciler.reconcile(raw, cat, null);

            assertThat(result.shop()).isNull();
        }
    }

    // ─────────────── 빈 카트 ───────────────

    @Nested
    @DisplayName("빈 카트")
    class EmptyCart {

        @Test
        @DisplayName("items 비어 있으면 totalPrice=0, items=[]")
        void emptyItems() {
            CartRaw raw = new CartRaw(1L, 1L, null, List.of());
            CatalogSnapshot cat = new CatalogSnapshot(Map.of(), Map.of());

            Cart result = reconciler.reconcile(raw, cat, null);

            assertThat(result.items()).isEmpty();
            assertThat(result.totalPrice()).isZero();
        }
    }
}
