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

    private static AddItemRequest newRequest(Long menuId, Integer quantity) {
        return new AddItemRequest("sess", menuId, "삼겹살 1인세트", quantity, List.of());
    }
}
