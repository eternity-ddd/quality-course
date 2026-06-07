package org.eternity.food.cart.command.service;

import org.eternity.food.Fixtures;
import org.eternity.food.base.generic.money.Money;
import org.eternity.food.cart.command.domain.Cart;
import org.eternity.food.cart.command.domain.CartLineItem;
import org.eternity.food.cart.command.domain.CartOption;
import org.eternity.food.cart.command.domain.CartOptionGroup;
import org.eternity.food.cart.command.domain.CartRepository;
import org.eternity.food.order.command.domain.Order;
import org.eternity.food.order.command.domain.OrderRepository;
import org.eternity.food.shop.command.domain.Menu;
import org.eternity.food.shop.command.domain.MenuStatus;
import org.eternity.food.shop.command.domain.Option;
import org.eternity.food.shop.command.domain.OptionGroup;
import org.eternity.food.shop.command.domain.Shop;
import org.eternity.food.shop.command.domain.ShopRepository;
import org.eternity.food.shop.command.domain.MenuRepository;
import org.eternity.food.shop.command.domain.OptionGroupRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * PlaceOrderService는 *OrderableCartInvariant 5 rules을 호출하는 orchestrator*.
 *
 * <p>여기서는 각 invariant 위반 시나리오 → ISE, 정상 흐름 → orderRepository.save 호출.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PlaceOrderService 단위 테스트")
class PlaceOrderServiceTest {

    @Mock
    private CartRepository cartRepository;

    @Mock
    private ShopRepository shopRepository;

    @Mock
    private MenuRepository menuRepository;

    @Mock
    private OptionGroupRepository optionGroupRepository;

    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private PlaceOrderService service;

    private Cart cart;
    private Shop shop;          // mock to control isOpen()
    private Menu menu;
    private OptionGroup optionGroup;

    @BeforeEach
    void setUp() {
        cart = Fixtures.aCart().build();

        shop = mock(Shop.class);
        lenient().when(shop.getId()).thenReturn(Fixtures.SHOP_ID);
        lenient().when(shop.isOpen()).thenReturn(true);
        // fixture cart는 basePrice 10,000 × count 1 = 10,000원 / shop minOrderPrice 13,000원
        // → 통과시키려면 충분한 값으로 stub해야 한다. 기본 5,000원 으로 deault.
        lenient().when(shop.getMinOrderPrice()).thenReturn(Money.wons(5_000));

        menu = Fixtures.aMenu().status(MenuStatus.OPEN).build();

        Option option = Fixtures.anOption().build();
        Option other = Fixtures.anOption().id(2L).name("대(500g)").price(Money.wons(20_000)).build();
        optionGroup = Fixtures.anOptionGroup().options(Set.of(option, other)).build();
    }

    private void wireHappyPath() {
        given(cartRepository.findByUserId(Fixtures.USER_ID)).willReturn(Optional.of(cart));
        given(shopRepository.findById(Fixtures.SHOP_ID)).willReturn(Optional.of(shop));
        given(menuRepository.findAllById(anyList())).willReturn(List.of(menu));
        given(optionGroupRepository.findAllById(anyList())).willReturn(List.of(optionGroup));
        given(orderRepository.save(any(Order.class))).willAnswer(inv -> inv.getArgument(0));
    }

    @Nested
    @DisplayName("정상 흐름")
    class HappyPath {

        @Test
        @DisplayName("Cart → Order 변환 + orderRepository.save 호출")
        void placeOrder_savesOrder() {
            wireHappyPath();

            Order order = service.placeOrder(Fixtures.USER_ID);

            assertThat(order).isNotNull();
            assertThat(order.getUserId()).isEqualTo(Fixtures.USER_ID);
            assertThat(order.getShopId()).isEqualTo(Fixtures.SHOP_ID);
            assertThat(order.getItems()).hasSize(cart.getItems().size());

            ArgumentCaptor<Order> captor = ArgumentCaptor.forClass(Order.class);
            verify(orderRepository).save(captor.capture());
            assertThat(captor.getValue().getItems())
                    .extracting(item -> item.menuId())
                    .containsExactly(Fixtures.MENU_ID);
        }
    }

    @Nested
    @DisplayName("선행 조건 위반")
    class Preconditions {

        @Test
        @DisplayName("cart 미존재 사용자면 IAE")
        void cartNotFound_throws() {
            given(cartRepository.findByUserId(Fixtures.USER_ID)).willReturn(Optional.empty());

            assertThatThrownBy(() -> service.placeOrder(Fixtures.USER_ID))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("카트는 존재하지 않는");
            verify(orderRepository, never()).save(any());
        }

        @Test
        @DisplayName("Cart.shopId가 null이면(빈 카트) ISE")
        void cartShopIdNull_throws() {
            Cart empty = Cart.forUser(Fixtures.USER_ID);
            given(cartRepository.findByUserId(Fixtures.USER_ID)).willReturn(Optional.of(empty));

            assertThatThrownBy(() -> service.placeOrder(Fixtures.USER_ID))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("장바구니가 비어");
            verify(orderRepository, never()).save(any());
        }

        @Test
        @DisplayName("shop 미존재면 IAE")
        void shopNotFound_throws() {
            given(cartRepository.findByUserId(Fixtures.USER_ID)).willReturn(Optional.of(cart));
            given(shopRepository.findById(Fixtures.SHOP_ID)).willReturn(Optional.empty());

            assertThatThrownBy(() -> service.placeOrder(Fixtures.USER_ID))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("가게를 찾을 수 없습니다");
            verify(orderRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("OrderableCartInvariant 위반 → ISE")
    class InvariantViolations {

        @Test
        @DisplayName("Rule 1: Cart가 비어 있으면 ISE")
        void rule1_emptyCart() {
            // Cart는 shopId 있으나 items 비어 있는 상태
            Cart emptyButShopSet = Fixtures.aCart().items(new java.util.ArrayList<>()).build();
            given(cartRepository.findByUserId(Fixtures.USER_ID))
                    .willReturn(Optional.of(emptyButShopSet));
            given(shopRepository.findById(Fixtures.SHOP_ID)).willReturn(Optional.of(shop));
            given(menuRepository.findAllById(anyList())).willReturn(List.of());
            given(optionGroupRepository.findAllById(anyList())).willReturn(List.of());

            assertThatThrownBy(() -> service.placeOrder(Fixtures.USER_ID))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("장바구니가 비어");
            verify(orderRepository, never()).save(any());
        }

        @Test
        @DisplayName("Rule 2: Shop ID 불일치면 ISE")
        void rule2_shopIdMismatch() {
            // Cart는 SHOP_ID(1L)을 갖고 있고, shopRepository는 다른 id로 stub
            Shop differentShop = mock(Shop.class);
            lenient().when(differentShop.getId()).thenReturn(999L);
            lenient().when(differentShop.isOpen()).thenReturn(true);
            lenient().when(differentShop.getMinOrderPrice()).thenReturn(Money.wons(1));

            given(cartRepository.findByUserId(Fixtures.USER_ID)).willReturn(Optional.of(cart));
            given(shopRepository.findById(Fixtures.SHOP_ID)).willReturn(Optional.of(differentShop));
            given(menuRepository.findAllById(anyList())).willReturn(List.of(menu));
            given(optionGroupRepository.findAllById(anyList())).willReturn(List.of(optionGroup));

            assertThatThrownBy(() -> service.placeOrder(Fixtures.USER_ID))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("가게와 장바구니의 가게가 일치하지");
            verify(orderRepository, never()).save(any());
        }

        @Test
        @DisplayName("Rule 3: Shop이 영업 중이 아니면 ISE")
        void rule3_shopClosed() {
            when(shop.isOpen()).thenReturn(false);

            given(cartRepository.findByUserId(Fixtures.USER_ID)).willReturn(Optional.of(cart));
            given(shopRepository.findById(Fixtures.SHOP_ID)).willReturn(Optional.of(shop));
            given(menuRepository.findAllById(anyList())).willReturn(List.of(menu));
            given(optionGroupRepository.findAllById(anyList())).willReturn(List.of(optionGroup));

            assertThatThrownBy(() -> service.placeOrder(Fixtures.USER_ID))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("영업중");
            verify(orderRepository, never()).save(any());
        }

        @Test
        @DisplayName("Rule 4: 최소 주문 금액 미달이면 ISE")
        void rule4_belowMinOrderPrice() {
            when(shop.getMinOrderPrice()).thenReturn(Money.wons(50_000_000));

            given(cartRepository.findByUserId(Fixtures.USER_ID)).willReturn(Optional.of(cart));
            given(shopRepository.findById(Fixtures.SHOP_ID)).willReturn(Optional.of(shop));
            given(menuRepository.findAllById(anyList())).willReturn(List.of(menu));
            given(optionGroupRepository.findAllById(anyList())).willReturn(List.of(optionGroup));

            assertThatThrownBy(() -> service.placeOrder(Fixtures.USER_ID))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("최소 주문금액");
            verify(orderRepository, never()).save(any());
        }

        @Test
        @DisplayName("Rule 5-a: 메뉴 미존재(menus에 없음) → ISE")
        void rule5_menuNotFound() {
            given(cartRepository.findByUserId(Fixtures.USER_ID)).willReturn(Optional.of(cart));
            given(shopRepository.findById(Fixtures.SHOP_ID)).willReturn(Optional.of(shop));
            given(menuRepository.findAllById(anyList())).willReturn(List.of());
            given(optionGroupRepository.findAllById(anyList())).willReturn(List.of(optionGroup));

            assertThatThrownBy(() -> service.placeOrder(Fixtures.USER_ID))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("메뉴를 찾을 수 없습니다");
            verify(orderRepository, never()).save(any());
        }

        @Test
        @DisplayName("Rule 5-b: 메뉴가 OPEN이 아니면 ISE")
        void rule5_menuNotOpen() {
            Menu readyMenu = Fixtures.aMenu().status(MenuStatus.READY).build();
            given(cartRepository.findByUserId(Fixtures.USER_ID)).willReturn(Optional.of(cart));
            given(shopRepository.findById(Fixtures.SHOP_ID)).willReturn(Optional.of(shop));
            given(menuRepository.findAllById(anyList())).willReturn(List.of(readyMenu));
            given(optionGroupRepository.findAllById(anyList())).willReturn(List.of(optionGroup));

            assertThatThrownBy(() -> service.placeOrder(Fixtures.USER_ID))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("판매중이 아닌 메뉴");
            verify(orderRepository, never()).save(any());
        }

        @Test
        @DisplayName("Rule 5-c: 옵션그룹이 catalog에서 사라짐 → ISE")
        void rule5_optionGroupRemoved() {
            given(cartRepository.findByUserId(Fixtures.USER_ID)).willReturn(Optional.of(cart));
            given(shopRepository.findById(Fixtures.SHOP_ID)).willReturn(Optional.of(shop));
            given(menuRepository.findAllById(anyList())).willReturn(List.of(menu));
            given(optionGroupRepository.findAllById(anyList())).willReturn(List.of());

            assertThatThrownBy(() -> service.placeOrder(Fixtures.USER_ID))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("옵션 그룹");
            verify(orderRepository, never()).save(any());
        }

        @Test
        @DisplayName("Rule 5-d: 옵션이 catalog에서 사라짐 → ISE")
        void rule5_optionRemoved() {
            // OptionGroup에 cart의 OPTION_ID와 다른 옵션 2개만 존재
            Option other1 = Fixtures.anOption().id(101L).name("미디움").price(Money.wons(11_000)).build();
            Option other2 = Fixtures.anOption().id(102L).name("라지").price(Money.wons(13_000)).build();
            OptionGroup grpWithoutOurOption = Fixtures.anOptionGroup()
                    .options(Set.of(other1, other2)).build();

            given(cartRepository.findByUserId(Fixtures.USER_ID)).willReturn(Optional.of(cart));
            given(shopRepository.findById(Fixtures.SHOP_ID)).willReturn(Optional.of(shop));
            given(menuRepository.findAllById(anyList())).willReturn(List.of(menu));
            given(optionGroupRepository.findAllById(anyList()))
                    .willReturn(List.of(grpWithoutOurOption));

            assertThatThrownBy(() -> service.placeOrder(Fixtures.USER_ID))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("옵션이 더 이상");
            verify(orderRepository, never()).save(any());
        }

        @Test
        @DisplayName("Rule 5-e: 옵션 가격 mismatch → ISE")
        void rule5_optionPriceMismatch() {
            // Cart에 담긴 OPTION_ID(1L)에 대응되는 catalog 옵션 가격이 변경됨
            Option changedPrice = Fixtures.anOption()
                    .id(Fixtures.OPTION_ID)
                    .name("소(250g)")
                    .price(Money.wons(99_999))
                    .build();
            Option pair = Fixtures.anOption().id(2L).name("대(500g)").price(Money.wons(20_000)).build();
            OptionGroup priceChanged = Fixtures.anOptionGroup()
                    .options(Set.of(changedPrice, pair)).build();

            given(cartRepository.findByUserId(Fixtures.USER_ID)).willReturn(Optional.of(cart));
            given(shopRepository.findById(Fixtures.SHOP_ID)).willReturn(Optional.of(shop));
            given(menuRepository.findAllById(anyList())).willReturn(List.of(menu));
            given(optionGroupRepository.findAllById(anyList())).willReturn(List.of(priceChanged));

            assertThatThrownBy(() -> service.placeOrder(Fixtures.USER_ID))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("옵션 가격이 변경");
            verify(orderRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Cart → Order 변환 위임")
    class Conversion {

        @Test
        @DisplayName("CartLineItem이 OrderLineItem으로 1:1 변환되며 옵션 정보가 보존된다")
        void converts_lineItems_preservingOptions() {
            wireHappyPath();

            service.placeOrder(Fixtures.USER_ID);

            ArgumentCaptor<Order> captor = ArgumentCaptor.forClass(Order.class);
            verify(orderRepository).save(captor.capture());

            Order saved = captor.getValue();
            assertThat(saved.getItems()).hasSize(1);
            assertThat(saved.getItems().get(0).menuName()).isEqualTo("삼겹살 1인세트");
            assertThat(saved.getItems().get(0).groups()).hasSize(1);
            assertThat(saved.getItems().get(0).groups().get(0).name()).isEqualTo("기본");
            assertThat(saved.getItems().get(0).groups().get(0).options())
                    .extracting(o -> o.name()).containsExactly("소(250g)");
            assertThat(saved.getItems().get(0).groups().get(0).options())
                    .extracting(o -> o.price()).containsExactly(12_000L);
        }

        @Test
        @DisplayName("여러 라인 → 모두 변환되어 Order에 포함")
        void converts_multipleLines() {
            // 두 번째 라인 추가
            CartOption opt = new CartOption("소(250g)", Money.wons(12_000));
            CartOptionGroup grp = new CartOptionGroup(Fixtures.OPTION_GROUP_ID, "기본", opt);
            CartLineItem second = CartLineItem.builder()
                    .id(2L)
                    .menuId(Fixtures.MENU_ID)
                    .menuName("삼겹살 1인세트")
                    .count(2)
                    .basePrice(Money.wons(15_000))
                    .groups(List.of(grp))
                    .build();
            Cart multi = Fixtures.aCart()
                    .items(List.of(Fixtures.aCartLineItem().build(), second))
                    .build();

            given(cartRepository.findByUserId(Fixtures.USER_ID)).willReturn(Optional.of(multi));
            given(shopRepository.findById(Fixtures.SHOP_ID)).willReturn(Optional.of(shop));
            given(menuRepository.findAllById(anyList())).willReturn(List.of(menu));
            given(optionGroupRepository.findAllById(anyList())).willReturn(List.of(optionGroup));
            given(orderRepository.save(any(Order.class))).willAnswer(inv -> inv.getArgument(0));

            service.placeOrder(Fixtures.USER_ID);

            ArgumentCaptor<Order> captor = ArgumentCaptor.forClass(Order.class);
            verify(orderRepository).save(captor.capture());
            assertThat(captor.getValue().getItems()).hasSize(2);
        }
    }
}
