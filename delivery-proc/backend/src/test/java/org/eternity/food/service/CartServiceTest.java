package org.eternity.food.service;

import org.eternity.food.Fixtures;
import org.eternity.food.dto.CartDto;
import org.eternity.food.dto.CartDto.AddItemRequest;
import org.eternity.food.dto.CartDto.AddItemRequest.SelectedOption;
import org.eternity.food.entity.Cart;
import org.eternity.food.entity.CartLineItem;
import org.eternity.food.entity.CartOption;
import org.eternity.food.entity.CartOptionGroup;
import org.eternity.food.entity.Menu;
import org.eternity.food.entity.MenuOptionGroup;
import org.eternity.food.entity.Option;
import org.eternity.food.entity.OptionGroup;
import org.eternity.food.entity.Order;
import org.eternity.food.entity.Shop;
import org.eternity.food.repository.CartRepository;
import org.eternity.food.repository.MenuRepository;
import org.eternity.food.repository.OptionGroupRepository;
import org.eternity.food.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("CartService 단위 테스트")
class CartServiceTest {

    @Mock
    private CartRepository cartRepository;

    @Mock
    private MenuRepository menuRepository;

    @Mock
    private OptionGroupRepository optionGroupRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ShopService shopService;

    @InjectMocks
    private CartService cartService;

    private Cart emptyCart;
    private Cart cartWithItem;
    private Menu menu;
    private OptionGroup optionGroup;
    private Option menuOption;
    private Shop shop;

    @BeforeEach
    void setUp() {
        emptyCart = Fixtures.aCart()
                .shopId(null)
                .items(new ArrayList<>())
                .build();

        menuOption = Fixtures.anOption().build();

        optionGroup = Fixtures.anOptionGroup()
                .options(new ArrayList<>(List.of(menuOption)))
                .build();

        MenuOptionGroup mog = Fixtures.aMenuOptionGroup().build();
        menu = Fixtures.aMenu()
                .optionGroups(new ArrayList<>(List.of(mog)))
                .build();

        shop = Fixtures.aShop().build();

        CartOption co = Fixtures.aCartOption().build();
        CartOptionGroup cog = Fixtures.aCartOptionGroup()
                .options(new ArrayList<>(List.of(co)))
                .build();

        CartLineItem line = Fixtures.aCartLineItem()
                .unitPrice(22_000L)
                .groups(new ArrayList<>(List.of(cog)))
                .build();

        cartWithItem = Fixtures.aCart()
                .shopId(Fixtures.SHOP_ID)
                .items(new ArrayList<>(List.of(line)))
                .build();
    }

    @Nested
    @DisplayName("addItem")
    class AddItem {

        @Test
        @DisplayName("userId null이면 IAE")
        void userId_null_throws() {
            AddItemRequest req = newRequest(menu.getId(), 1);

            assertThatThrownBy(() -> cartService.addItem(null, req))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("사용자 ID");

            verify(cartRepository, never()).save(any());
        }

        @Test
        @DisplayName("userId null + request null이어도 userId 체크가 먼저 (진입점 순서)")
        void userId_null_takesPrecedence_overRequestNull() {
            assertThatThrownBy(() -> cartService.addItem(null, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("사용자 ID");
        }

        @Test
        @DisplayName("userId 유효 + 정상 흐름이면 IAE 발생 안 함")
        void userId_valid_doesNotThrow() {
            given(cartRepository.findByUserId(Fixtures.USER_ID)).willReturn(Optional.of(emptyCart));
            given(cartRepository.save(any())).willReturn(emptyCart);
            given(menuRepository.findById(menu.getId())).willReturn(Optional.of(menu));
            given(menuRepository.findAllById(any())).willReturn(List.of(menu));
            given(optionGroupRepository.findAllById(any())).willReturn(List.of(optionGroup));
            given(shopService.loadShopOrThrow(menu.getShopId())).willReturn(shop);

            AddItemRequest req = newRequest(menu.getId(), 1);

            cartService.addItem(Fixtures.USER_ID, req);

            verify(cartRepository).save(any(Cart.class));
        }

        @Test
        @DisplayName("request null이면 IAE")
        void request_null_throws() {
            assertThatThrownBy(() -> cartService.addItem(Fixtures.USER_ID, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("장바구니 아이템 요청");
        }

        @Test
        @DisplayName("request.menuId null이면 IAE")
        void request_menuId_null_throws() {
            AddItemRequest req = new AddItemRequest("sess", null, "이름", 1, List.of());

            assertThatThrownBy(() -> cartService.addItem(Fixtures.USER_ID, req))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("메뉴 ID");
        }

        @Test
        @DisplayName("request 유효 + menu 부재면 그 다음 단계 IAE 발생")
        void request_valid_menu_missing_throws() {
            given(cartRepository.findByUserId(Fixtures.USER_ID)).willReturn(Optional.empty());
            given(cartRepository.save(any())).willReturn(emptyCart);
            given(menuRepository.findById(999L)).willReturn(Optional.empty());

            AddItemRequest req = newRequest(999L, 1);

            assertThatThrownBy(() -> cartService.addItem(Fixtures.USER_ID, req))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("메뉴를 찾을 수 없습니다");
        }

        @Test
        @DisplayName("menu.shopId null이면 IAE")
        void menu_shopId_null_throws() {
            Menu orphan = Fixtures.aMenu().shopId(null).build();
            given(cartRepository.findByUserId(Fixtures.USER_ID)).willReturn(Optional.of(emptyCart));
            given(menuRepository.findById(menu.getId())).willReturn(Optional.of(orphan));

            AddItemRequest req = newRequest(menu.getId(), 1);

            assertThatThrownBy(() -> cartService.addItem(Fixtures.USER_ID, req))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("가게 ID");
        }

        @Test
        @DisplayName("menu.shopId null + 빈 카트여도 검증이 먼저")
        void menu_shopId_null_emptyCart_stillThrows() {
            Menu orphan = Fixtures.aMenu().shopId(null).status("OPEN").build();
            given(cartRepository.findByUserId(Fixtures.USER_ID)).willReturn(Optional.empty());
            given(cartRepository.save(any())).willAnswer(inv -> inv.getArgument(0));
            given(menuRepository.findById(menu.getId())).willReturn(Optional.of(orphan));

            assertThatThrownBy(() -> cartService.addItem(Fixtures.USER_ID, newRequest(menu.getId(), 1)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("가게");
        }

        @Test
        @DisplayName("menu.shopId 정상이면 통과")
        void menu_shopId_valid_passes() {
            given(cartRepository.findByUserId(Fixtures.USER_ID)).willReturn(Optional.of(emptyCart));
            given(cartRepository.save(any())).willReturn(emptyCart);
            given(menuRepository.findById(menu.getId())).willReturn(Optional.of(menu));
            given(menuRepository.findAllById(any())).willReturn(List.of(menu));
            given(optionGroupRepository.findAllById(any())).willReturn(List.of(optionGroup));
            given(shopService.loadShopOrThrow(menu.getShopId())).willReturn(shop);

            cartService.addItem(Fixtures.USER_ID, newRequest(menu.getId(), 1));

            verify(cartRepository).save(any(Cart.class));
        }

        @Test
        @DisplayName("quantity 0이면 IAE")
        void quantity_zero_throws() {
            AddItemRequest req = newRequest(menu.getId(), 0);

            assertThatThrownBy(() -> cartService.addItem(Fixtures.USER_ID, req))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("수량");
        }

        @Test
        @DisplayName("quantity 음수면 IAE")
        void quantity_negative_throws() {
            AddItemRequest req = newRequest(menu.getId(), -1);

            assertThatThrownBy(() -> cartService.addItem(Fixtures.USER_ID, req))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("수량");
        }

        @Test
        @DisplayName("quantity null이면 IAE")
        void quantity_null_throws() {
            AddItemRequest req = new AddItemRequest("s", menu.getId(), "이름", null, List.of());

            assertThatThrownBy(() -> cartService.addItem(Fixtures.USER_ID, req))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("수량");
        }

        @Test
        @DisplayName("quantity 1 이상이면 정상 흐름 진행")
        void quantity_one_ok() {
            given(cartRepository.findByUserId(Fixtures.USER_ID)).willReturn(Optional.of(emptyCart));
            given(cartRepository.save(any())).willReturn(emptyCart);
            given(menuRepository.findById(menu.getId())).willReturn(Optional.of(menu));
            given(menuRepository.findAllById(any())).willReturn(List.of(menu));
            given(optionGroupRepository.findAllById(any())).willReturn(List.of(optionGroup));
            given(shopService.loadShopOrThrow(menu.getShopId())).willReturn(shop);

            cartService.addItem(Fixtures.USER_ID, newRequest(menu.getId(), 1));

            ArgumentCaptor<Cart> captor = ArgumentCaptor.forClass(Cart.class);
            verify(cartRepository).save(captor.capture());
            assertThat(captor.getValue().getItems()).hasSize(1);
            assertThat(captor.getValue().getItems().get(0).getMenuCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("카트에 다른 shop 라인 있으면 clear 후 새 shop 라인만 남음")
        void differentShop_clearsExistingItems() {
            Cart existing = Fixtures.aCart()
                    .shopId(99L)
                    .items(new ArrayList<>(List.of(Fixtures.aCartLineItem().menuId(77L).build())))
                    .build();

            given(cartRepository.findByUserId(Fixtures.USER_ID)).willReturn(Optional.of(existing));
            given(cartRepository.save(any())).willAnswer(inv -> inv.getArgument(0));
            given(menuRepository.findById(menu.getId())).willReturn(Optional.of(menu));
            given(menuRepository.findAllById(any())).willReturn(List.of(menu));
            given(optionGroupRepository.findAllById(any())).willReturn(List.of(optionGroup));
            given(shopService.loadShopOrThrow(menu.getShopId())).willReturn(shop);

            cartService.addItem(Fixtures.USER_ID, newRequest(menu.getId(), 1));

            ArgumentCaptor<Cart> captor = ArgumentCaptor.forClass(Cart.class);
            verify(cartRepository).save(captor.capture());
            Cart saved = captor.getValue();
            assertThat(saved.getShopId()).isEqualTo(menu.getShopId());
            assertThat(saved.getItems()).hasSize(1);
            assertThat(saved.getItems().get(0).getMenuId()).isEqualTo(menu.getId());
        }

        @Test
        @DisplayName("같은 shop이면 clear 발생하지 않고 라인 누적")
        void sameShop_keepsExisting() {
            Menu existingMenu = Fixtures.aMenu().id(50L).build();
            Cart existing = Fixtures.aCart()
                    .shopId(Fixtures.SHOP_ID)
                    .items(new ArrayList<>(List.of(
                            Fixtures.aCartLineItem().id(10L).menuId(existingMenu.getId()).build())))
                    .build();

            given(cartRepository.findByUserId(Fixtures.USER_ID)).willReturn(Optional.of(existing));
            given(cartRepository.save(any())).willAnswer(inv -> inv.getArgument(0));
            given(menuRepository.findById(menu.getId())).willReturn(Optional.of(menu));
            given(menuRepository.findAllById(any())).willReturn(List.of(menu, existingMenu));
            given(optionGroupRepository.findAllById(any())).willReturn(List.of(optionGroup));
            given(shopService.loadShopOrThrow(menu.getShopId())).willReturn(shop);

            cartService.addItem(Fixtures.USER_ID, newRequest(menu.getId(), 1));

            ArgumentCaptor<Cart> captor = ArgumentCaptor.forClass(Cart.class);
            verify(cartRepository).save(captor.capture());
            assertThat(captor.getValue().getItems()).hasSize(2);
        }

        @Test
        @DisplayName("빈 카트(shopId null)에 추가하면 shopId만 세팅")
        void emptyCart_setsShopId() {
            given(cartRepository.findByUserId(Fixtures.USER_ID)).willReturn(Optional.of(emptyCart));
            given(cartRepository.save(any())).willAnswer(inv -> inv.getArgument(0));
            given(menuRepository.findById(menu.getId())).willReturn(Optional.of(menu));
            given(menuRepository.findAllById(any())).willReturn(List.of(menu));
            given(optionGroupRepository.findAllById(any())).willReturn(List.of(optionGroup));
            given(shopService.loadShopOrThrow(menu.getShopId())).willReturn(shop);

            cartService.addItem(Fixtures.USER_ID, newRequest(menu.getId(), 1));

            ArgumentCaptor<Cart> captor = ArgumentCaptor.forClass(Cart.class);
            verify(cartRepository).save(captor.capture());
            assertThat(captor.getValue().getShopId()).isEqualTo(menu.getShopId());
            assertThat(captor.getValue().getItems()).hasSize(1);
        }

        @Test
        @DisplayName("menu 부재면 IAE")
        void menu_not_found_throws() {
            given(cartRepository.findByUserId(Fixtures.USER_ID)).willReturn(Optional.of(emptyCart));
            given(menuRepository.findById(menu.getId())).willReturn(Optional.empty());

            assertThatThrownBy(() -> cartService.addItem(Fixtures.USER_ID, newRequest(menu.getId(), 1)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("메뉴를 찾을 수 없습니다");
        }

        @Test
        @DisplayName("menu가 OPEN이 아니면 ISE")
        void menu_not_open_throws() {
            Menu closed = Fixtures.aMenu().status("READY").build();
            given(cartRepository.findByUserId(Fixtures.USER_ID)).willReturn(Optional.of(emptyCart));
            given(menuRepository.findById(menu.getId())).willReturn(Optional.of(closed));

            assertThatThrownBy(() -> cartService.addItem(Fixtures.USER_ID, newRequest(menu.getId(), 1)))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("판매중이 아닌");
        }

        @Test
        @DisplayName("cart가 없으면 새로 생성")
        void cart_autocreated_when_missing() {
            given(cartRepository.findByUserId(Fixtures.USER_ID)).willReturn(Optional.empty());
            given(cartRepository.save(any())).willAnswer(inv -> {
                Cart c = inv.getArgument(0);
                c.setId(99L);
                return c;
            });
            given(menuRepository.findById(menu.getId())).willReturn(Optional.of(menu));
            given(menuRepository.findAllById(any())).willReturn(List.of(menu));
            given(optionGroupRepository.findAllById(any())).willReturn(List.of(optionGroup));
            given(shopService.loadShopOrThrow(menu.getShopId())).willReturn(shop);

            cartService.addItem(Fixtures.USER_ID, newRequest(menu.getId(), 1));

            verify(cartRepository, org.mockito.Mockito.atLeastOnce()).save(any(Cart.class));
        }

        @Test
        @DisplayName("선택된 옵션 그룹이 메뉴에 속하지 않으면 IAE")
        void option_group_not_in_menu_throws() {
            given(cartRepository.findByUserId(Fixtures.USER_ID)).willReturn(Optional.of(emptyCart));
            given(menuRepository.findById(menu.getId())).willReturn(Optional.of(menu));
            SelectedOption bad = new SelectedOption(999L, "딴그룹", 1L, "옵션", 0L);
            AddItemRequest req = new AddItemRequest("s", menu.getId(), "이름", 1, List.of(bad));

            assertThatThrownBy(() -> cartService.addItem(Fixtures.USER_ID, req))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("옵션 그룹");
        }

        @Test
        @DisplayName("선택된 옵션이 옵션그룹에 속하지 않으면 IAE")
        void option_not_in_group_throws() {
            given(cartRepository.findByUserId(Fixtures.USER_ID)).willReturn(Optional.of(emptyCart));
            given(menuRepository.findById(menu.getId())).willReturn(Optional.of(menu));
            given(optionGroupRepository.findAllById(any())).willReturn(List.of(optionGroup));

            SelectedOption bad = new SelectedOption(
                    Fixtures.OPTION_GROUP_ID, "기본", 9999L, "헛것", 0L);
            AddItemRequest req = new AddItemRequest("s", menu.getId(), "이름", 1, List.of(bad));

            assertThatThrownBy(() -> cartService.addItem(Fixtures.USER_ID, req))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("옵션");
        }

        @Test
        @DisplayName("선택된 옵션 그룹이 메뉴 카탈로그에는 등록됐지만 OptionGroup이 DB에서 사라진 경우 IAE")
        void option_group_id_present_in_menu_but_missing_in_db_throws() {
            given(cartRepository.findByUserId(Fixtures.USER_ID)).willReturn(Optional.of(emptyCart));
            given(menuRepository.findById(menu.getId())).willReturn(Optional.of(menu));
            given(optionGroupRepository.findAllById(any())).willReturn(List.of());

            SelectedOption sel = new SelectedOption(
                    Fixtures.OPTION_GROUP_ID, "기본",
                    Fixtures.OPTION_ID, "소(250g)", 12_000L);
            AddItemRequest req = new AddItemRequest("s", menu.getId(), "이름", 1, List.of(sel));

            assertThatThrownBy(() -> cartService.addItem(Fixtures.USER_ID, req))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("존재하지 않는 옵션 그룹");
        }

        @Test
        @DisplayName("request.menuName이 null이면 menu.getName()으로 fallback")
        void menuName_null_fallsBackToMenuName() {
            given(cartRepository.findByUserId(Fixtures.USER_ID)).willReturn(Optional.of(emptyCart));
            given(cartRepository.save(any())).willAnswer(inv -> inv.getArgument(0));
            given(menuRepository.findById(menu.getId())).willReturn(Optional.of(menu));
            given(menuRepository.findAllById(any())).willReturn(List.of(menu));
            given(optionGroupRepository.findAllById(any())).willReturn(List.of(optionGroup));
            given(shopService.loadShopOrThrow(menu.getShopId())).willReturn(shop);

            AddItemRequest req = new AddItemRequest("s", menu.getId(), null, 1, List.of());

            cartService.addItem(Fixtures.USER_ID, req);

            ArgumentCaptor<Cart> captor = ArgumentCaptor.forClass(Cart.class);
            verify(cartRepository).save(captor.capture());
            CartLineItem line = captor.getValue().getItems().get(0);
            assertThat(line.getMenuName()).isEqualTo(menu.getName());
        }

        @Test
        @DisplayName("선택된 옵션의 price가 null이면 0L로 저장")
        void selectedOption_priceNull_treatedAsZero() {
            given(cartRepository.findByUserId(Fixtures.USER_ID)).willReturn(Optional.of(emptyCart));
            given(cartRepository.save(any())).willAnswer(inv -> inv.getArgument(0));
            given(menuRepository.findById(menu.getId())).willReturn(Optional.of(menu));
            given(menuRepository.findAllById(any())).willReturn(List.of(menu));
            given(optionGroupRepository.findAllById(any())).willReturn(List.of(optionGroup));
            given(shopService.loadShopOrThrow(menu.getShopId())).willReturn(shop);

            SelectedOption sel = new SelectedOption(
                    Fixtures.OPTION_GROUP_ID, "기본",
                    Fixtures.OPTION_ID, "소(250g)", null);
            AddItemRequest req = new AddItemRequest("s", menu.getId(), "삼겹살", 1, List.of(sel));

            cartService.addItem(Fixtures.USER_ID, req);

            ArgumentCaptor<Cart> captor = ArgumentCaptor.forClass(Cart.class);
            verify(cartRepository).save(captor.capture());
            CartLineItem line = captor.getValue().getItems().get(0);
            assertThat(line.getGroups().get(0).getOptions().get(0).getPrice()).isEqualTo(0L);
            assertThat(line.getUnitPrice()).isEqualTo(10_000L);
        }

        @Test
        @DisplayName("selectedOptions가 null이어도 정상 진행 — 옵션 없는 라인으로 생성")
        void selectedOptions_null_isTreatedAsEmpty() {
            given(cartRepository.findByUserId(Fixtures.USER_ID)).willReturn(Optional.of(emptyCart));
            given(cartRepository.save(any())).willAnswer(inv -> inv.getArgument(0));
            given(menuRepository.findById(menu.getId())).willReturn(Optional.of(menu));
            given(menuRepository.findAllById(any())).willReturn(List.of(menu));
            given(optionGroupRepository.findAllById(any())).willReturn(List.of(optionGroup));
            given(shopService.loadShopOrThrow(menu.getShopId())).willReturn(shop);

            AddItemRequest req = new AddItemRequest("s", menu.getId(), "삼겹살", 1, null);

            cartService.addItem(Fixtures.USER_ID, req);

            ArgumentCaptor<Cart> captor = ArgumentCaptor.forClass(Cart.class);
            verify(cartRepository).save(captor.capture());
            CartLineItem line = captor.getValue().getItems().get(0);
            assertThat(line.getGroups()).isEmpty();
            assertThat(line.getUnitPrice()).isEqualTo(10_000L);
        }

        @Test
        @DisplayName("정상 흐름 — line 생성 + unitPrice = basePrice + 옵션 가격 합산")
        void happyPath_buildsLineWithCorrectUnitPrice() {
            given(cartRepository.findByUserId(Fixtures.USER_ID)).willReturn(Optional.of(emptyCart));
            given(cartRepository.save(any())).willAnswer(inv -> inv.getArgument(0));
            given(menuRepository.findById(menu.getId())).willReturn(Optional.of(menu));
            given(menuRepository.findAllById(any())).willReturn(List.of(menu));
            given(optionGroupRepository.findAllById(any())).willReturn(List.of(optionGroup));
            given(shopService.loadShopOrThrow(menu.getShopId())).willReturn(shop);

            SelectedOption sel = new SelectedOption(
                    Fixtures.OPTION_GROUP_ID, "기본",
                    Fixtures.OPTION_ID, "소(250g)", 12_000L);
            AddItemRequest req = new AddItemRequest("s", menu.getId(), "삼겹살 1인세트", 2, List.of(sel));

            cartService.addItem(Fixtures.USER_ID, req);

            ArgumentCaptor<Cart> captor = ArgumentCaptor.forClass(Cart.class);
            verify(cartRepository).save(captor.capture());
            Cart saved = captor.getValue();
            assertThat(saved.getItems()).hasSize(1);
            CartLineItem line = saved.getItems().get(0);
            assertThat(line.getMenuId()).isEqualTo(menu.getId());
            assertThat(line.getMenuCount()).isEqualTo(2);
            assertThat(line.getUnitPrice()).isEqualTo(22_000L);
            assertThat(line.getGroups()).hasSize(1);
            assertThat(line.getGroups().get(0).getOptions()).hasSize(1);
        }

        @Test
        @DisplayName("동일 내용 라인 추가 — 새 라인 생성 대신 기존 라인 count 누적")
        void sameContent_combinesCount() {
            given(cartRepository.findByUserId(Fixtures.USER_ID)).willReturn(Optional.of(cartWithItem));
            given(cartRepository.save(any())).willAnswer(inv -> inv.getArgument(0));
            given(menuRepository.findById(menu.getId())).willReturn(Optional.of(menu));
            given(menuRepository.findAllById(any())).willReturn(List.of(menu));
            given(optionGroupRepository.findAllById(any())).willReturn(List.of(optionGroup));
            given(shopService.loadShopOrThrow(menu.getShopId())).willReturn(shop);

            SelectedOption sel = new SelectedOption(
                    Fixtures.OPTION_GROUP_ID, "기본",
                    Fixtures.OPTION_ID, "소(250g)", 12_000L);
            AddItemRequest req = new AddItemRequest("s", menu.getId(), "삼겹살 1인세트", 3, List.of(sel));

            cartService.addItem(Fixtures.USER_ID, req);

            ArgumentCaptor<Cart> captor = ArgumentCaptor.forClass(Cart.class);
            verify(cartRepository).save(captor.capture());
            Cart saved = captor.getValue();
            assertThat(saved.getItems()).hasSize(1);
            assertThat(saved.getItems().get(0).getMenuCount()).isEqualTo(1 + 3);
        }

        @Test
        @DisplayName("다른 옵션 선택 — 동일 메뉴여도 새 라인 생성")
        void differentOptions_addsNewLine() {
            given(cartRepository.findByUserId(Fixtures.USER_ID)).willReturn(Optional.of(cartWithItem));
            given(cartRepository.save(any())).willAnswer(inv -> inv.getArgument(0));
            given(menuRepository.findById(menu.getId())).willReturn(Optional.of(menu));
            given(menuRepository.findAllById(any())).willReturn(List.of(menu));
            given(optionGroupRepository.findAllById(any())).willReturn(List.of(optionGroup));
            given(shopService.loadShopOrThrow(menu.getShopId())).willReturn(shop);

            AddItemRequest req = new AddItemRequest("s", menu.getId(), "삼겹살 1인세트", 1, List.of());

            cartService.addItem(Fixtures.USER_ID, req);

            ArgumentCaptor<Cart> captor = ArgumentCaptor.forClass(Cart.class);
            verify(cartRepository).save(captor.capture());
            assertThat(captor.getValue().getItems()).hasSize(2);
        }
    }

    @Nested
    @DisplayName("updateQuantity")
    class UpdateQuantity {

        @Test
        @DisplayName("quantity null이면 IAE")
        void quantity_null_throws() {
            assertThatThrownBy(() -> cartService.updateQuantity(
                    Fixtures.USER_ID, Fixtures.CART_LINE_ITEM_ID, null, "s"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("수량");
        }

        @Test
        @DisplayName("quantity 음수면 IAE")
        void quantity_negative_throws() {
            assertThatThrownBy(() -> cartService.updateQuantity(
                    Fixtures.USER_ID, Fixtures.CART_LINE_ITEM_ID, -1, "s"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("0 이상");
        }

        @Test
        @DisplayName("quantity 음수 -100도 IAE (경계값)")
        void quantity_largeNegative_throws() {
            assertThatThrownBy(() -> cartService.updateQuantity(
                    Fixtures.USER_ID, Fixtures.CART_LINE_ITEM_ID, -100, "s"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("0 이상");
        }

        @Test
        @DisplayName("quantity 0이면 해당 라인 자동 제거")
        void quantity_zero_removesLine() {
            given(cartRepository.findByUserId(Fixtures.USER_ID)).willReturn(Optional.of(cartWithItem));
            given(cartRepository.save(any())).willAnswer(inv -> inv.getArgument(0));
            given(shopService.loadShopOrThrow(anyLong())).willReturn(shop);
            given(menuRepository.findAllById(any())).willReturn(List.of(menu));
            given(optionGroupRepository.findAllById(any())).willReturn(List.of(optionGroup));

            cartService.updateQuantity(Fixtures.USER_ID, Fixtures.CART_LINE_ITEM_ID, 0, "s");

            ArgumentCaptor<Cart> captor = ArgumentCaptor.forClass(Cart.class);
            verify(cartRepository).save(captor.capture());
            assertThat(captor.getValue().getItems()).isEmpty();
        }

        @Test
        @DisplayName("마지막 라인 제거되면 shopId도 null로 리셋")
        void quantity_zero_lastItem_resetsShopId() {
            given(cartRepository.findByUserId(Fixtures.USER_ID)).willReturn(Optional.of(cartWithItem));
            given(cartRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

            cartService.updateQuantity(Fixtures.USER_ID, Fixtures.CART_LINE_ITEM_ID, 0, "s");

            ArgumentCaptor<Cart> captor = ArgumentCaptor.forClass(Cart.class);
            verify(cartRepository).save(captor.capture());
            assertThat(captor.getValue().getItems()).isEmpty();
            assertThat(captor.getValue().getShopId()).isNull();
        }

        @Test
        @DisplayName("quantity=0인데 itemId가 없으면 NO-OP (제거할 게 없음)")
        void quantity_zero_unknownItem_isNoOp() {
            given(cartRepository.findByUserId(Fixtures.USER_ID)).willReturn(Optional.of(cartWithItem));
            given(cartRepository.save(any())).willAnswer(inv -> inv.getArgument(0));
            given(shopService.loadShopOrThrow(anyLong())).willReturn(shop);
            given(menuRepository.findAllById(any())).willReturn(List.of(menu));
            given(optionGroupRepository.findAllById(any())).willReturn(List.of(optionGroup));

            cartService.updateQuantity(Fixtures.USER_ID, 99999L, 0, "s");

            ArgumentCaptor<Cart> captor = ArgumentCaptor.forClass(Cart.class);
            verify(cartRepository).save(captor.capture());
            assertThat(captor.getValue().getItems()).hasSize(1);
        }

        @Test
        @DisplayName("quantity 양수면 해당 라인 menuCount 변경")
        void quantity_positive_updatesCount() {
            given(cartRepository.findByUserId(Fixtures.USER_ID)).willReturn(Optional.of(cartWithItem));
            given(cartRepository.save(any())).willAnswer(inv -> inv.getArgument(0));
            given(shopService.loadShopOrThrow(anyLong())).willReturn(shop);
            given(menuRepository.findAllById(any())).willReturn(List.of(menu));
            given(optionGroupRepository.findAllById(any())).willReturn(List.of(optionGroup));

            cartService.updateQuantity(Fixtures.USER_ID, Fixtures.CART_LINE_ITEM_ID, 5, "s");

            ArgumentCaptor<Cart> captor = ArgumentCaptor.forClass(Cart.class);
            verify(cartRepository).save(captor.capture());
            assertThat(captor.getValue().getItems().get(0).getMenuCount()).isEqualTo(5);
        }

        @Test
        @DisplayName("quantity 양수 + itemId 없으면 IAE")
        void quantity_positive_unknownItem_throws() {
            given(cartRepository.findByUserId(Fixtures.USER_ID)).willReturn(Optional.of(cartWithItem));

            assertThatThrownBy(() -> cartService.updateQuantity(
                    Fixtures.USER_ID, 99999L, 3, "s"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("카트 아이템");
        }

        @Test
        @DisplayName("cart 부재면 IAE")
        void cart_missing_throws() {
            given(cartRepository.findByUserId(Fixtures.USER_ID)).willReturn(Optional.empty());

            assertThatThrownBy(() -> cartService.updateQuantity(
                    Fixtures.USER_ID, Fixtures.CART_LINE_ITEM_ID, 1, "s"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("카트");
        }
    }

    @Nested
    @DisplayName("removeItem")
    class RemoveItem {

        @Test
        @DisplayName("정상 — 라인 제거 + getCart 호출")
        void happyPath_removesItem() {
            CartLineItem line2 = Fixtures.aCartLineItem().id(2L).menuId(50L).build();
            Cart twoItems = Fixtures.aCart()
                    .shopId(Fixtures.SHOP_ID)
                    .items(new ArrayList<>(List.of(cartWithItem.getItems().get(0), line2)))
                    .build();
            given(cartRepository.findByUserId(Fixtures.USER_ID)).willReturn(Optional.of(twoItems));
            given(cartRepository.save(any())).willAnswer(inv -> inv.getArgument(0));
            given(shopService.loadShopOrThrow(anyLong())).willReturn(shop);
            given(menuRepository.findAllById(any())).willReturn(List.of(menu));
            given(optionGroupRepository.findAllById(any())).willReturn(List.of(optionGroup));

            cartService.removeItem(Fixtures.USER_ID, Fixtures.CART_LINE_ITEM_ID, "s");

            ArgumentCaptor<Cart> captor = ArgumentCaptor.forClass(Cart.class);
            verify(cartRepository).save(captor.capture());
            assertThat(captor.getValue().getItems()).hasSize(1);
            assertThat(captor.getValue().getItems().get(0).getId()).isEqualTo(2L);
        }

        @Test
        @DisplayName("itemId가 없으면 IAE")
        void unknownItem_throws() {
            given(cartRepository.findByUserId(Fixtures.USER_ID)).willReturn(Optional.of(cartWithItem));

            assertThatThrownBy(() -> cartService.removeItem(Fixtures.USER_ID, 9999L, "s"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("카트 아이템");
        }

        @Test
        @DisplayName("cart 부재면 IAE")
        void cart_missing_throws() {
            given(cartRepository.findByUserId(Fixtures.USER_ID)).willReturn(Optional.empty());

            assertThatThrownBy(() -> cartService.removeItem(Fixtures.USER_ID, 1L, "s"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("카트");
        }

        @Test
        @DisplayName("마지막 라인 제거 시 shopId null로 리셋")
        void lastItem_resetsShopId() {
            given(cartRepository.findByUserId(Fixtures.USER_ID)).willReturn(Optional.of(cartWithItem));
            given(cartRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

            cartService.removeItem(Fixtures.USER_ID, Fixtures.CART_LINE_ITEM_ID, "s");

            ArgumentCaptor<Cart> captor = ArgumentCaptor.forClass(Cart.class);
            verify(cartRepository).save(captor.capture());
            assertThat(captor.getValue().getItems()).isEmpty();
            assertThat(captor.getValue().getShopId()).isNull();
        }
    }

    @Nested
    @DisplayName("placeOrder")
    class PlaceOrder {

        @Test
        @DisplayName("카트 라인이 비어 있으면 ISE")
        void emptyItems_throws() {
            Cart empty = Fixtures.aCart()
                    .shopId(null)
                    .items(new ArrayList<>())
                    .build();
            given(cartRepository.findByUserId(Fixtures.USER_ID)).willReturn(Optional.of(empty));

            assertThatThrownBy(() -> cartService.placeOrder(Fixtures.USER_ID))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("장바구니가 비어");
        }

        @Test
        @DisplayName("shopId 있어도 라인 비면 ISE")
        void emptyItems_withShopId_throws() {
            Cart empty = Fixtures.aCart()
                    .shopId(Fixtures.SHOP_ID)
                    .items(new ArrayList<>())
                    .build();
            given(cartRepository.findByUserId(Fixtures.USER_ID)).willReturn(Optional.of(empty));
            given(shopService.loadShopOrThrow(Fixtures.SHOP_ID)).willReturn(shop);

            assertThatThrownBy(() -> cartService.placeOrder(Fixtures.USER_ID))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("장바구니가 비어");
        }

        @Test
        @DisplayName("라인이 있어야 통과 (정상 흐름의 일부)")
        void withItems_passes() {
            stubHappyPlaceOrder();
            cartService.placeOrder(Fixtures.USER_ID);
            verify(orderRepository).save(any(Order.class));
        }

        @Test
        @DisplayName("cart.shopId == null이면 ISE (라인은 있는 상태)")
        void cartShopId_null_throws() {
            CartLineItem line = cartWithItem.getItems().get(0);
            Cart bad = Fixtures.aCart()
                    .shopId(null)
                    .items(new ArrayList<>(List.of(line)))
                    .build();
            given(cartRepository.findByUserId(Fixtures.USER_ID)).willReturn(Optional.of(bad));
            given(menuRepository.findAllById(any())).willReturn(List.of(menu));
            given(optionGroupRepository.findAllById(any())).willReturn(List.of(optionGroup));

            assertThatThrownBy(() -> cartService.placeOrder(Fixtures.USER_ID))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("일치하지");
        }

        @Test
        @DisplayName("cart.shopId != shop.id이면 ISE")
        void cartShopId_mismatch_throws() {
            Shop otherShop = Fixtures.aShop().id(999L).build();
            given(cartRepository.findByUserId(Fixtures.USER_ID)).willReturn(Optional.of(cartWithItem));
            given(shopService.loadShopOrThrow(Fixtures.SHOP_ID)).willReturn(otherShop);
            given(menuRepository.findAllById(any())).willReturn(List.of(menu));
            given(optionGroupRepository.findAllById(any())).willReturn(List.of(optionGroup));

            assertThatThrownBy(() -> cartService.placeOrder(Fixtures.USER_ID))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("일치하지");
        }

        @Test
        @DisplayName("shopId 일치하면 통과")
        void shopId_match_passes() {
            stubHappyPlaceOrder();
            cartService.placeOrder(Fixtures.USER_ID);
            verify(orderRepository).save(any(Order.class));
        }

        @Test
        @DisplayName("shop이 영업 중이 아니면 ISE")
        void shopClosed_throws() {
            given(cartRepository.findByUserId(Fixtures.USER_ID)).willReturn(Optional.of(cartWithItem));
            given(shopService.loadShopOrThrow(Fixtures.SHOP_ID)).willReturn(shop);
            given(menuRepository.findAllById(any())).willReturn(List.of(menu));
            given(optionGroupRepository.findAllById(any())).willReturn(List.of(optionGroup));
            given(shopService.isShopOpen(shop)).willReturn(false);

            assertThatThrownBy(() -> cartService.placeOrder(Fixtures.USER_ID))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("영업");
        }

        @Test
        @DisplayName("isShopOpen==true이면 통과")
        void shopOpen_passes() {
            stubHappyPlaceOrder();
            cartService.placeOrder(Fixtures.USER_ID);
            verify(orderRepository).save(any(Order.class));
        }

        @Test
        @DisplayName("닫힌 가게면 Order 저장 안 됨")
        void shopClosed_orderNotSaved() {
            given(cartRepository.findByUserId(Fixtures.USER_ID)).willReturn(Optional.of(cartWithItem));
            given(shopService.loadShopOrThrow(Fixtures.SHOP_ID)).willReturn(shop);
            given(menuRepository.findAllById(any())).willReturn(List.of(menu));
            given(optionGroupRepository.findAllById(any())).willReturn(List.of(optionGroup));
            given(shopService.isShopOpen(shop)).willReturn(false);

            assertThatThrownBy(() -> cartService.placeOrder(Fixtures.USER_ID))
                    .isInstanceOf(IllegalStateException.class);
            verify(orderRepository, never()).save(any());
        }

        @Test
        @DisplayName("totalPrice < minOrderPrice이면 ISE")
        void belowMinOrderPrice_throws() {
            Shop expensiveShop = Fixtures.aShop().minOrderPrice(100_000L).build();
            given(cartRepository.findByUserId(Fixtures.USER_ID)).willReturn(Optional.of(cartWithItem));
            given(shopService.loadShopOrThrow(Fixtures.SHOP_ID)).willReturn(expensiveShop);
            given(shopService.isShopOpen(expensiveShop)).willReturn(true);
            given(menuRepository.findAllById(any())).willReturn(List.of(menu));
            given(optionGroupRepository.findAllById(any())).willReturn(List.of(optionGroup));

            assertThatThrownBy(() -> cartService.placeOrder(Fixtures.USER_ID))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("최소 주문금액");
        }

        @Test
        @DisplayName("totalPrice == minOrderPrice이면 통과 (경계)")
        void exactlyMinOrderPrice_passes() {
            Shop exactShop = Fixtures.aShop().minOrderPrice(22_000L).build();
            given(cartRepository.findByUserId(Fixtures.USER_ID)).willReturn(Optional.of(cartWithItem));
            given(shopService.loadShopOrThrow(Fixtures.SHOP_ID)).willReturn(exactShop);
            given(shopService.isShopOpen(exactShop)).willReturn(true);
            given(menuRepository.findAllById(any())).willReturn(List.of(menu));
            given(optionGroupRepository.findAllById(any())).willReturn(List.of(optionGroup));
            given(orderRepository.save(any())).willAnswer(inv -> {
                Order o = inv.getArgument(0);
                o.setId(1L);
                return o;
            });
            given(cartRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

            cartService.placeOrder(Fixtures.USER_ID);

            verify(orderRepository).save(any(Order.class));
        }

        @Test
        @DisplayName("totalPrice > minOrderPrice이면 통과")
        void aboveMinOrderPrice_passes() {
            stubHappyPlaceOrder();
            cartService.placeOrder(Fixtures.USER_ID);
            verify(orderRepository).save(any(Order.class));
        }

        @Test
        @DisplayName("shop.minOrderPrice가 null이면 최소금액 검증을 skip하고 통과")
        void minOrderPrice_null_skipsCheck() {
            Shop noMinShop = Fixtures.aShop().minOrderPrice(null).build();
            given(cartRepository.findByUserId(Fixtures.USER_ID)).willReturn(Optional.of(cartWithItem));
            given(shopService.loadShopOrThrow(Fixtures.SHOP_ID)).willReturn(noMinShop);
            given(shopService.isShopOpen(noMinShop)).willReturn(true);
            given(menuRepository.findAllById(any())).willReturn(List.of(menu));
            given(optionGroupRepository.findAllById(any())).willReturn(List.of(optionGroup));
            given(orderRepository.save(any())).willAnswer(inv -> {
                Order o = inv.getArgument(0);
                o.setId(1L);
                return o;
            });
            given(cartRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

            cartService.placeOrder(Fixtures.USER_ID);

            verify(orderRepository).save(any(Order.class));
        }

        @Test
        @DisplayName("메뉴가 카탈로그에서 사라졌으면 ISE")
        void menuRemoved_throws() {
            given(cartRepository.findByUserId(Fixtures.USER_ID)).willReturn(Optional.of(cartWithItem));
            given(shopService.loadShopOrThrow(Fixtures.SHOP_ID)).willReturn(shop);
            given(shopService.isShopOpen(shop)).willReturn(true);
            given(menuRepository.findAllById(any())).willReturn(List.of());
            given(optionGroupRepository.findAllById(any())).willReturn(List.of());

            assertThatThrownBy(() -> cartService.placeOrder(Fixtures.USER_ID))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("메뉴를 찾을 수 없습니다");
        }

        @Test
        @DisplayName("메뉴가 OPEN이 아니면 ISE")
        void menuNotOpen_throws() {
            Menu closed = Fixtures.aMenu()
                    .status("READY")
                    .optionGroups(new ArrayList<>(List.of(Fixtures.aMenuOptionGroup().build())))
                    .build();
            given(cartRepository.findByUserId(Fixtures.USER_ID)).willReturn(Optional.of(cartWithItem));
            given(shopService.loadShopOrThrow(Fixtures.SHOP_ID)).willReturn(shop);
            given(shopService.isShopOpen(shop)).willReturn(true);
            given(menuRepository.findAllById(any())).willReturn(List.of(closed));
            given(optionGroupRepository.findAllById(any())).willReturn(List.of(optionGroup));

            assertThatThrownBy(() -> cartService.placeOrder(Fixtures.USER_ID))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("판매중이 아닌");
        }

        @Test
        @DisplayName("옵션 그룹이 메뉴 카탈로그에서 사라졌으면 ISE")
        void optionGroupRemoved_throws() {
            given(cartRepository.findByUserId(Fixtures.USER_ID)).willReturn(Optional.of(cartWithItem));
            given(shopService.loadShopOrThrow(Fixtures.SHOP_ID)).willReturn(shop);
            given(shopService.isShopOpen(shop)).willReturn(true);
            given(menuRepository.findAllById(any())).willReturn(List.of(menu));
            given(optionGroupRepository.findAllById(any())).willReturn(List.of());

            assertThatThrownBy(() -> cartService.placeOrder(Fixtures.USER_ID))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("옵션 그룹");
        }

        @Test
        @DisplayName("옵션이 옵션그룹에서 사라졌으면 ISE")
        void optionRemoved_throws() {
            OptionGroup emptyOg = Fixtures.anOptionGroup()
                    .options(new ArrayList<>())
                    .build();
            given(cartRepository.findByUserId(Fixtures.USER_ID)).willReturn(Optional.of(cartWithItem));
            given(shopService.loadShopOrThrow(Fixtures.SHOP_ID)).willReturn(shop);
            given(shopService.isShopOpen(shop)).willReturn(true);
            given(menuRepository.findAllById(any())).willReturn(List.of(menu));
            given(optionGroupRepository.findAllById(any())).willReturn(List.of(emptyOg));

            assertThatThrownBy(() -> cartService.placeOrder(Fixtures.USER_ID))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("옵션");
        }

        @Test
        @DisplayName("옵션 가격이 변경됐으면 ISE")
        void priceChanged_throws() {
            Option changedPrice = Fixtures.anOption().price(15_000L).build();
            OptionGroup ogChanged = Fixtures.anOptionGroup()
                    .options(new ArrayList<>(List.of(changedPrice)))
                    .build();
            given(cartRepository.findByUserId(Fixtures.USER_ID)).willReturn(Optional.of(cartWithItem));
            given(shopService.loadShopOrThrow(Fixtures.SHOP_ID)).willReturn(shop);
            given(shopService.isShopOpen(shop)).willReturn(true);
            given(menuRepository.findAllById(any())).willReturn(List.of(menu));
            given(optionGroupRepository.findAllById(any())).willReturn(List.of(ogChanged));

            assertThatThrownBy(() -> cartService.placeOrder(Fixtures.USER_ID))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("가격");
        }

        @Test
        @DisplayName("line.unitPrice가 null이면 ISE")
        void unitPriceNull_throws() {
            CartOption co = Fixtures.aCartOption().build();
            CartOptionGroup cog = Fixtures.aCartOptionGroup()
                    .options(new ArrayList<>(List.of(co)))
                    .build();
            CartLineItem nullPriceLine = Fixtures.aCartLineItem()
                    .unitPrice(null)
                    .groups(new ArrayList<>(List.of(cog)))
                    .build();
            Cart bad = Fixtures.aCart()
                    .shopId(Fixtures.SHOP_ID)
                    .items(new ArrayList<>(List.of(nullPriceLine)))
                    .build();

            given(cartRepository.findByUserId(Fixtures.USER_ID)).willReturn(Optional.of(bad));
            given(shopService.loadShopOrThrow(Fixtures.SHOP_ID)).willReturn(shop);
            given(shopService.isShopOpen(shop)).willReturn(true);
            given(menuRepository.findAllById(any())).willReturn(List.of(menu));
            given(optionGroupRepository.findAllById(any())).willReturn(List.of(optionGroup));

            assertThatThrownBy(() -> cartService.placeOrder(Fixtures.USER_ID))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("가격");
        }

        @Test
        @DisplayName("line.unitPrice가 재계산된 unit과 다르면 ISE")
        void unitPriceMismatch_throws() {
            CartOption co = Fixtures.aCartOption().build();
            CartOptionGroup cog = Fixtures.aCartOptionGroup()
                    .options(new ArrayList<>(List.of(co)))
                    .build();
            CartLineItem wrong = Fixtures.aCartLineItem()
                    .unitPrice(11_111L)
                    .groups(new ArrayList<>(List.of(cog)))
                    .build();
            Cart bad = Fixtures.aCart()
                    .shopId(Fixtures.SHOP_ID)
                    .items(new ArrayList<>(List.of(wrong)))
                    .build();

            given(cartRepository.findByUserId(Fixtures.USER_ID)).willReturn(Optional.of(bad));
            given(shopService.loadShopOrThrow(Fixtures.SHOP_ID)).willReturn(shop);
            given(shopService.isShopOpen(shop)).willReturn(true);
            given(menuRepository.findAllById(any())).willReturn(List.of(menu));
            given(optionGroupRepository.findAllById(any())).willReturn(List.of(optionGroup));

            assertThatThrownBy(() -> cartService.placeOrder(Fixtures.USER_ID))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("가격");
        }

        @Test
        @DisplayName("정상 — Order 저장됨")
        void happyPath_savesOrder() {
            stubHappyPlaceOrder();

            CartDto.OrderPlacedResponse response = cartService.placeOrder(Fixtures.USER_ID);

            assertThat(response.orderId()).isNotNull();
            assertThat(response.totalPrice()).isEqualTo(22_000L);
            ArgumentCaptor<Order> captor = ArgumentCaptor.forClass(Order.class);
            verify(orderRepository).save(captor.capture());
            assertThat(captor.getValue().getUserId()).isEqualTo(Fixtures.USER_ID);
            assertThat(captor.getValue().getShopId()).isEqualTo(Fixtures.SHOP_ID);
            assertThat(captor.getValue().getItemsSnapshot()).hasSize(1);
            assertThat(captor.getValue().getTotalPrice()).isEqualTo(22_000L);
        }

        @Test
        @DisplayName("placeOrder 후 cart.items + shopId 동시 reset")
        void happyPath_clearsCart() {
            stubHappyPlaceOrder();

            cartService.placeOrder(Fixtures.USER_ID);

            ArgumentCaptor<Cart> captor = ArgumentCaptor.forClass(Cart.class);
            verify(cartRepository).save(captor.capture());
            Cart savedCart = captor.getValue();
            assertThat(savedCart.getItems()).isEmpty();
            assertThat(savedCart.getShopId()).isNull();
        }

        @Test
        @DisplayName("cart 부재면 IAE")
        void cart_missing_throws() {
            given(cartRepository.findByUserId(Fixtures.USER_ID)).willReturn(Optional.empty());

            assertThatThrownBy(() -> cartService.placeOrder(Fixtures.USER_ID))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("카트");
        }

        private void stubHappyPlaceOrder() {
            given(cartRepository.findByUserId(Fixtures.USER_ID)).willReturn(Optional.of(cartWithItem));
            given(shopService.loadShopOrThrow(Fixtures.SHOP_ID)).willReturn(shop);
            given(shopService.isShopOpen(shop)).willReturn(true);
            given(menuRepository.findAllById(any())).willReturn(List.of(menu));
            given(optionGroupRepository.findAllById(any())).willReturn(List.of(optionGroup));
            given(orderRepository.save(any())).willAnswer(inv -> {
                Order o = inv.getArgument(0);
                o.setId(Fixtures.ORDER_ID);
                return o;
            });
            given(cartRepository.save(any())).willAnswer(inv -> inv.getArgument(0));
        }
    }

    @Nested
    @DisplayName("getTotalPrice")
    class GetTotalPrice {

        @Test
        @DisplayName("cart 부재 — 0 반환")
        void cart_missing_returnsZero() {
            given(cartRepository.findByUserId(Fixtures.USER_ID)).willReturn(Optional.empty());

            assertThat(cartService.getTotalPrice(Fixtures.USER_ID)).isEqualTo(0L);
        }

        @Test
        @DisplayName("cart 비어있음 — 0 반환")
        void cart_emptyItems_returnsZero() {
            given(cartRepository.findByUserId(Fixtures.USER_ID)).willReturn(Optional.of(emptyCart));

            assertThat(cartService.getTotalPrice(Fixtures.USER_ID)).isEqualTo(0L);
        }

        @Test
        @DisplayName("단일 라인 — basePrice + 옵션 가격 합산")
        void singleLine_sumsCorrectly() {
            given(cartRepository.findByUserId(Fixtures.USER_ID)).willReturn(Optional.of(cartWithItem));
            given(menuRepository.findAllById(any())).willReturn(List.of(menu));
            given(optionGroupRepository.findAllById(any())).willReturn(List.of(optionGroup));

            assertThat(cartService.getTotalPrice(Fixtures.USER_ID)).isEqualTo(22_000L);
        }

        @Test
        @DisplayName("다중 라인 — 라인별 (basePrice + 옵션) × count 합산")
        void multipleLines_sumsCorrectly() {
            Option opt2 = Fixtures.anOption().id(2L).optionGroupId(2L).name("대(500g)").price(8_000L).build();
            OptionGroup og2 = Fixtures.anOptionGroup().id(2L).name("추가").required(false)
                    .options(new ArrayList<>(List.of(opt2))).build();

            Menu menu2 = Fixtures.aMenu().id(2L).name("목살 세트").basePrice(15_000L)
                    .optionGroups(new ArrayList<>(List.of(
                            Fixtures.aMenuOptionGroup().optionGroupId(2L).displayOrder(1).build())))
                    .build();

            CartOption co2 = Fixtures.aCartOption().id(2L).optionId(2L).name("대(500g)").price(8_000L).build();
            CartOptionGroup cog2 = Fixtures.aCartOptionGroup().id(2L).optionGroupId(2L).name("추가")
                    .options(new ArrayList<>(List.of(co2))).build();
            CartLineItem line2 = Fixtures.aCartLineItem().id(2L).menuId(2L).menuName("목살 세트")
                    .unitPrice(23_000L).menuCount(2)
                    .groups(new ArrayList<>(List.of(cog2))).build();

            Cart multiCart = Fixtures.aCart()
                    .shopId(Fixtures.SHOP_ID)
                    .items(new ArrayList<>(List.of(cartWithItem.getItems().get(0), line2)))
                    .build();

            given(cartRepository.findByUserId(Fixtures.USER_ID)).willReturn(Optional.of(multiCart));
            given(menuRepository.findAllById(any())).willReturn(List.of(menu, menu2));
            given(optionGroupRepository.findAllById(any())).willReturn(List.of(optionGroup, og2));

            assertThat(cartService.getTotalPrice(Fixtures.USER_ID)).isEqualTo(68_000L);
        }

        @Test
        @DisplayName("메뉴가 카탈로그에서 사라졌으면 저장된 unitPrice 기준으로 합산")
        void menuRemoved_usesStoredUnitPrice() {
            given(cartRepository.findByUserId(Fixtures.USER_ID)).willReturn(Optional.of(cartWithItem));
            given(menuRepository.findAllById(any())).willReturn(List.of());
            given(optionGroupRepository.findAllById(any())).willReturn(List.of());

            assertThat(cartService.getTotalPrice(Fixtures.USER_ID)).isEqualTo(22_000L);
        }
    }

    @Nested
    @DisplayName("getCart")
    class GetCart {

        @Test
        @DisplayName("cart 부재 — empty CartResponse 반환")
        void cart_missing_returnsEmpty() {
            given(cartRepository.findByUserId(Fixtures.USER_ID)).willReturn(Optional.empty());

            CartDto.CartResponse res = cartService.getCart(Fixtures.USER_ID, "sess");

            assertThat(res.id()).isNull();
            assertThat(res.sessionId()).isEqualTo("sess");
            assertThat(res.items()).isEmpty();
            assertThat(res.totalPrice()).isEqualTo(0L);
            assertThat(res.shop()).isNull();
        }

        @Test
        @DisplayName("cart 있으나 items 비어있음 — items=empty, totalPrice=0")
        void cart_emptyItems_returnsEmptyItems() {
            given(cartRepository.findByUserId(Fixtures.USER_ID))
                    .willReturn(Optional.of(emptyCart));

            CartDto.CartResponse res = cartService.getCart(Fixtures.USER_ID, "s");

            assertThat(res.items()).isEmpty();
            assertThat(res.totalPrice()).isEqualTo(0L);
            assertThat(res.shop()).isNull();
        }

        @Test
        @DisplayName("정상 — VALID 라인 1개")
        void valid_returnsValidItem() {
            given(cartRepository.findByUserId(Fixtures.USER_ID)).willReturn(Optional.of(cartWithItem));
            given(menuRepository.findAllById(any())).willReturn(List.of(menu));
            given(optionGroupRepository.findAllById(any())).willReturn(List.of(optionGroup));
            given(shopService.loadShopOrThrow(Fixtures.SHOP_ID)).willReturn(shop);
            given(shopService.isShopOpen(shop)).willReturn(true);

            CartDto.CartResponse res = cartService.getCart(Fixtures.USER_ID, "s");

            assertThat(res.items()).hasSize(1);
            CartDto.Item item = res.items().get(0);
            assertThat(item.status()).isEqualTo(CartDto.ItemStatus.VALID);
            assertThat(item.unitPrice()).isEqualTo(22_000L);
            assertThat(res.totalPrice()).isEqualTo(22_000L);
            assertThat(res.shop()).isNotNull();
            assertThat(res.shop().open()).isTrue();
        }

        @Test
        @DisplayName("다중 라인 — totalPrice는 라인별 (unitPrice × count) 합산")
        void multipleLines_totalPrice_sumsCorrectly() {
            Option opt2 = Fixtures.anOption().id(2L).optionGroupId(2L).name("대(500g)").price(8_000L).build();
            OptionGroup og2 = Fixtures.anOptionGroup().id(2L).name("추가").required(false)
                    .options(new ArrayList<>(List.of(opt2))).build();

            Menu menu2 = Fixtures.aMenu().id(2L).name("목살 세트").basePrice(15_000L)
                    .optionGroups(new ArrayList<>(List.of(
                            Fixtures.aMenuOptionGroup().optionGroupId(2L).displayOrder(1).build())))
                    .build();

            CartOption co2 = Fixtures.aCartOption().id(2L).optionId(2L).name("대(500g)").price(8_000L).build();
            CartOptionGroup cog2 = Fixtures.aCartOptionGroup().id(2L).optionGroupId(2L).name("추가")
                    .options(new ArrayList<>(List.of(co2))).build();
            CartLineItem line2 = Fixtures.aCartLineItem().id(2L).menuId(2L).menuName("목살 세트")
                    .unitPrice(23_000L).menuCount(2)
                    .groups(new ArrayList<>(List.of(cog2))).build();

            Cart multiCart = Fixtures.aCart()
                    .shopId(Fixtures.SHOP_ID)
                    .items(new ArrayList<>(List.of(cartWithItem.getItems().get(0), line2)))
                    .build();

            given(cartRepository.findByUserId(Fixtures.USER_ID)).willReturn(Optional.of(multiCart));
            given(menuRepository.findAllById(any())).willReturn(List.of(menu, menu2));
            given(optionGroupRepository.findAllById(any())).willReturn(List.of(optionGroup, og2));
            given(shopService.loadShopOrThrow(Fixtures.SHOP_ID)).willReturn(shop);
            given(shopService.isShopOpen(shop)).willReturn(true);

            CartDto.CartResponse res = cartService.getCart(Fixtures.USER_ID, "s");

            assertThat(res.items()).hasSize(2);
            // line1: base 10_000 + opt 12_000 = 22_000 × 1 = 22_000
            // line2: base 15_000 + opt 8_000 = 23_000 × 2 = 46_000
            assertThat(res.totalPrice()).isEqualTo(68_000L);
        }

        @Test
        @DisplayName("menu가 카탈로그에서 사라졌으면 MENU_REMOVED")
        void menuRemoved_returnsMenuRemoved() {
            given(cartRepository.findByUserId(Fixtures.USER_ID)).willReturn(Optional.of(cartWithItem));
            given(menuRepository.findAllById(any())).willReturn(List.of());
            given(optionGroupRepository.findAllById(any())).willReturn(List.of());
            given(shopService.loadShopOrThrow(Fixtures.SHOP_ID)).willReturn(shop);
            given(shopService.isShopOpen(shop)).willReturn(false);

            CartDto.CartResponse res = cartService.getCart(Fixtures.USER_ID, "s");

            assertThat(res.items()).hasSize(1);
            assertThat(res.items().get(0).status()).isEqualTo(CartDto.ItemStatus.MENU_REMOVED);
            assertThat(res.items().get(0).messages()).isNotEmpty();
        }

        @Test
        @DisplayName("menu가 OPEN이 아니면 MENU_NOT_OPEN")
        void menuNotOpen_returnsMenuNotOpen() {
            Menu ready = Fixtures.aMenu()
                    .status("READY")
                    .optionGroups(new ArrayList<>(List.of(Fixtures.aMenuOptionGroup().build())))
                    .build();
            given(cartRepository.findByUserId(Fixtures.USER_ID)).willReturn(Optional.of(cartWithItem));
            given(menuRepository.findAllById(any())).willReturn(List.of(ready));
            given(optionGroupRepository.findAllById(any())).willReturn(List.of(optionGroup));
            given(shopService.loadShopOrThrow(Fixtures.SHOP_ID)).willReturn(shop);
            given(shopService.isShopOpen(shop)).willReturn(true);

            CartDto.CartResponse res = cartService.getCart(Fixtures.USER_ID, "s");

            assertThat(res.items().get(0).status()).isEqualTo(CartDto.ItemStatus.MENU_NOT_OPEN);
        }

        @Test
        @DisplayName("옵션 가격이 변경되면 PRICE_CHANGED")
        void priceChanged_returnsPriceChanged() {
            Option higher = Fixtures.anOption().price(13_000L).build();
            OptionGroup og = Fixtures.anOptionGroup()
                    .options(new ArrayList<>(List.of(higher)))
                    .build();
            given(cartRepository.findByUserId(Fixtures.USER_ID)).willReturn(Optional.of(cartWithItem));
            given(menuRepository.findAllById(any())).willReturn(List.of(menu));
            given(optionGroupRepository.findAllById(any())).willReturn(List.of(og));
            given(shopService.loadShopOrThrow(Fixtures.SHOP_ID)).willReturn(shop);
            given(shopService.isShopOpen(shop)).willReturn(true);

            CartDto.CartResponse res = cartService.getCart(Fixtures.USER_ID, "s");

            assertThat(res.items().get(0).status()).isEqualTo(CartDto.ItemStatus.PRICE_CHANGED);
        }

        @Test
        @DisplayName("cart 비어있고 shopId 있으면 ShopBrief 채워서 빈 items 반환")
        void emptyItems_withShopId_loadsShopBrief() {
            Cart emptyWithShop = Fixtures.aCart()
                    .shopId(Fixtures.SHOP_ID)
                    .items(new ArrayList<>())
                    .build();
            given(cartRepository.findByUserId(Fixtures.USER_ID)).willReturn(Optional.of(emptyWithShop));
            given(shopService.loadShopOrThrow(Fixtures.SHOP_ID)).willReturn(shop);
            given(shopService.isShopOpen(shop)).willReturn(true);

            CartDto.CartResponse res = cartService.getCart(Fixtures.USER_ID, "s");

            assertThat(res.items()).isEmpty();
            assertThat(res.totalPrice()).isEqualTo(0L);
            assertThat(res.shop()).isNotNull();
            assertThat(res.shop().id()).isEqualTo(Fixtures.SHOP_ID);
        }

        @Test
        @DisplayName("정상 라인이지만 cart.shopId가 null이면 shop=null로 응답")
        void validItem_butShopIdNull_returnsNullShop() {
            Cart noShop = Fixtures.aCart()
                    .shopId(null)
                    .items(new ArrayList<>(List.of(cartWithItem.getItems().get(0))))
                    .build();
            given(cartRepository.findByUserId(Fixtures.USER_ID)).willReturn(Optional.of(noShop));
            given(menuRepository.findAllById(any())).willReturn(List.of(menu));
            given(optionGroupRepository.findAllById(any())).willReturn(List.of(optionGroup));

            CartDto.CartResponse res = cartService.getCart(Fixtures.USER_ID, "s");

            assertThat(res.items()).hasSize(1);
            assertThat(res.shop()).isNull();
        }

        @Test
        @DisplayName("메뉴 이름이 변경되면 messages에 변경 안내 추가")
        void menuNameChanged_addsMessage() {
            Menu renamedMenu = Fixtures.aMenu().name("프리미엄 삼겹살 1인세트").build();
            given(cartRepository.findByUserId(Fixtures.USER_ID)).willReturn(Optional.of(cartWithItem));
            given(menuRepository.findAllById(any())).willReturn(List.of(renamedMenu));
            given(optionGroupRepository.findAllById(any())).willReturn(List.of(optionGroup));
            given(shopService.loadShopOrThrow(Fixtures.SHOP_ID)).willReturn(shop);
            given(shopService.isShopOpen(shop)).willReturn(true);

            CartDto.CartResponse res = cartService.getCart(Fixtures.USER_ID, "s");

            CartDto.Item item = res.items().get(0);
            assertThat(item.menuName()).isEqualTo("프리미엄 삼겹살 1인세트");
            assertThat(item.messages()).anyMatch(m -> m.contains("메뉴 이름이 변경"));
        }

        @Test
        @DisplayName("옵션 그룹이 카탈로그에서 사라지면 옵션은 INVALID, 라인은 INVALID_OPTION")
        void optionGroupRemoved_marksOptionsInvalid() {
            given(cartRepository.findByUserId(Fixtures.USER_ID)).willReturn(Optional.of(cartWithItem));
            given(menuRepository.findAllById(any())).willReturn(List.of(menu));
            given(optionGroupRepository.findAllById(any())).willReturn(List.of());
            given(shopService.loadShopOrThrow(Fixtures.SHOP_ID)).willReturn(shop);
            given(shopService.isShopOpen(shop)).willReturn(true);

            CartDto.CartResponse res = cartService.getCart(Fixtures.USER_ID, "s");

            CartDto.Item item = res.items().get(0);
            assertThat(item.status()).isEqualTo(CartDto.ItemStatus.INVALID_OPTION);
            assertThat(item.selectedOptions()).hasSize(1);
            assertThat(item.selectedOptions().get(0).status()).isEqualTo(CartDto.OptionStatus.INVALID);
            assertThat(item.messages()).anyMatch(m -> m.contains("옵션이 더 이상"));
        }

        @Test
        @DisplayName("옵션 그룹이 메뉴에 속하지 않게 바뀌어도 옵션은 INVALID")
        void optionGroupNotInMenu_marksOptionsInvalid() {
            Menu menuWithoutGroup = Fixtures.aMenu()
                    .optionGroups(new ArrayList<>())
                    .build();
            given(cartRepository.findByUserId(Fixtures.USER_ID)).willReturn(Optional.of(cartWithItem));
            given(menuRepository.findAllById(any())).willReturn(List.of(menuWithoutGroup));
            given(optionGroupRepository.findAllById(any())).willReturn(List.of(optionGroup));
            given(shopService.loadShopOrThrow(Fixtures.SHOP_ID)).willReturn(shop);
            given(shopService.isShopOpen(shop)).willReturn(true);

            CartDto.CartResponse res = cartService.getCart(Fixtures.USER_ID, "s");

            CartDto.Item item = res.items().get(0);
            assertThat(item.status()).isEqualTo(CartDto.ItemStatus.INVALID_OPTION);
            assertThat(item.selectedOptions()).hasSize(1);
            assertThat(item.selectedOptions().get(0).status()).isEqualTo(CartDto.OptionStatus.INVALID);
        }

        @Test
        @DisplayName("옵션 그룹 이름이 변경되면 messages에 변경 안내 추가")
        void optionGroupNameChanged_addsMessage() {
            OptionGroup renamed = Fixtures.anOptionGroup()
                    .name("프리미엄 중량")
                    .options(new ArrayList<>(List.of(menuOption)))
                    .build();
            given(cartRepository.findByUserId(Fixtures.USER_ID)).willReturn(Optional.of(cartWithItem));
            given(menuRepository.findAllById(any())).willReturn(List.of(menu));
            given(optionGroupRepository.findAllById(any())).willReturn(List.of(renamed));
            given(shopService.loadShopOrThrow(Fixtures.SHOP_ID)).willReturn(shop);
            given(shopService.isShopOpen(shop)).willReturn(true);

            CartDto.CartResponse res = cartService.getCart(Fixtures.USER_ID, "s");

            CartDto.Item item = res.items().get(0);
            assertThat(item.messages()).anyMatch(m -> m.contains("옵션 그룹 이름이 변경"));
            assertThat(item.status()).isEqualTo(CartDto.ItemStatus.VALID);
        }

        @Test
        @DisplayName("개별 옵션이 사라지면 옵션은 INVALID, 라인은 INVALID_OPTION")
        void singleOptionRemoved_marksThatOptionInvalid() {
            OptionGroup emptyOg = Fixtures.anOptionGroup()
                    .options(new ArrayList<>())
                    .build();
            given(cartRepository.findByUserId(Fixtures.USER_ID)).willReturn(Optional.of(cartWithItem));
            given(menuRepository.findAllById(any())).willReturn(List.of(menu));
            given(optionGroupRepository.findAllById(any())).willReturn(List.of(emptyOg));
            given(shopService.loadShopOrThrow(Fixtures.SHOP_ID)).willReturn(shop);
            given(shopService.isShopOpen(shop)).willReturn(true);

            CartDto.CartResponse res = cartService.getCart(Fixtures.USER_ID, "s");

            CartDto.Item item = res.items().get(0);
            assertThat(item.status()).isEqualTo(CartDto.ItemStatus.INVALID_OPTION);
            assertThat(item.selectedOptions()).hasSize(1);
            assertThat(item.selectedOptions().get(0).status()).isEqualTo(CartDto.OptionStatus.INVALID);
        }

        @Test
        @DisplayName("옵션 가격은 동일하나 이름만 변경되면 OptionStatus.NAME_UPDATED")
        void onlyOptionNameChanged_marksNameUpdated() {
            Option renamedOpt = Fixtures.anOption().name("소(250g 신메뉴명)").build();
            OptionGroup ogRenamed = Fixtures.anOptionGroup()
                    .options(new ArrayList<>(List.of(renamedOpt)))
                    .build();
            given(cartRepository.findByUserId(Fixtures.USER_ID)).willReturn(Optional.of(cartWithItem));
            given(menuRepository.findAllById(any())).willReturn(List.of(menu));
            given(optionGroupRepository.findAllById(any())).willReturn(List.of(ogRenamed));
            given(shopService.loadShopOrThrow(Fixtures.SHOP_ID)).willReturn(shop);
            given(shopService.isShopOpen(shop)).willReturn(true);

            CartDto.CartResponse res = cartService.getCart(Fixtures.USER_ID, "s");

            CartDto.Item item = res.items().get(0);
            assertThat(item.status()).isEqualTo(CartDto.ItemStatus.VALID);
            assertThat(item.selectedOptions()).hasSize(1);
            assertThat(item.selectedOptions().get(0).status()).isEqualTo(CartDto.OptionStatus.NAME_UPDATED);
            assertThat(item.selectedOptions().get(0).name()).isEqualTo("소(250g 신메뉴명)");
        }
    }

    private static AddItemRequest newRequest(Long menuId, Integer quantity) {
        return new AddItemRequest("sess", menuId, "삼겹살 1인세트", quantity, List.of());
    }
}
