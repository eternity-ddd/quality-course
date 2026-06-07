package org.eternity.food.service;

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
import org.eternity.food.entity.Shop;
import org.eternity.food.repository.CartRepository;
import org.eternity.food.repository.MenuRepository;
import org.eternity.food.repository.OptionGroupRepository;
import org.eternity.food.repository.OrderRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
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

    @Test
    @DisplayName("다중 라인 — 라인별 (basePrice + 옵션) × count 합산")
    void multipleLines_sumsCorrectly() {
        // 라인1: basePrice 10,000 + 옵션 12,000 = 22,000 × 1
        CartLineItem line1 = CartLineItem.builder().menuId(1L).basePrice(10_000L).menuCount(1)
                .groups(List.of(CartOptionGroup.builder().optionGroupId(1L)
                        .options(List.of(CartOption.builder().optionId(1L).price(12_000L).build())).build()))
                .build();
        // 라인2: basePrice 15,000 + 옵션 8,000 = 23,000 × 2
        CartLineItem line2 = CartLineItem.builder().menuId(2L).basePrice(15_000L).menuCount(2)
                .groups(List.of(CartOptionGroup.builder().optionGroupId(2L)
                        .options(List.of(CartOption.builder().optionId(2L).price(8_000L).build())).build()))
                .build();
        // 합산: 22,000 + 46,000 = 68,000
        Cart cart = Cart.builder().items(new ArrayList<>(List.of(line1, line2))).build();

        given(cartRepository.findByUserId(1L)).willReturn(Optional.of(cart));

        assertThat(cartService.getTotalPrice(1L)).isEqualTo(68_000L);
    }

    @Test
    @DisplayName("카트에 다른 shop 라인 있으면 clear 후 새 shop 라인만 남음")
    void differentShop_clearsExistingItems() {
        Menu menu = Menu.builder().id(1L).shopId(1L).name("삼겹살").basePrice(10_000L).status("OPEN")
                .optionGroups(List.of(MenuOptionGroup.builder().optionGroupId(1L).build())).build();
        OptionGroup og = OptionGroup.builder().id(1L)
                .options(List.of(Option.builder().id(1L).price(12_000L).build())).build();
        Shop shop = Shop.builder().id(1L).name("오겹돼지").minOrderPrice(13_000L).build();

        Cart existing = Cart.builder().userId(1L).shopId(99L)
                .items(new ArrayList<>(List.of(
                        CartLineItem.builder().menuId(77L).menuCount(1).basePrice(0L).build())))
                .build();

        given(cartRepository.findByUserId(1L)).willReturn(Optional.of(existing));
        given(cartRepository.save(any())).willAnswer(inv -> inv.getArgument(0));
        given(menuRepository.findById(1L)).willReturn(Optional.of(menu));
        given(menuRepository.findAllById(any())).willReturn(List.of(menu));
        given(optionGroupRepository.findAllById(any())).willReturn(List.of(og));
        given(shopService.loadShopOrThrow(1L)).willReturn(shop);

        cartService.addItem(1L, new AddItemRequest("sess", 1L, "삼겹살", 1, List.of()));

        verify(cartRepository).save(any(Cart.class));
        assertThat(existing.getShopId()).isEqualTo(1L);
        assertThat(existing.getItems()).hasSize(1);
        assertThat(existing.getItems().get(0).getMenuId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("동일 내용 라인 추가 — 새 라인 생성 대신 기존 라인 count 누적")
    void sameContent_combines() {
        Menu menu = Menu.builder().id(1L).shopId(1L).name("삼겹살 1인세트").basePrice(10_000L).status("OPEN")
                .optionGroups(List.of(MenuOptionGroup.builder().optionGroupId(1L).build())).build();
        OptionGroup og = OptionGroup.builder().id(1L).name("기본")
                .options(List.of(Option.builder().id(1L).name("소(250g)").price(12_000L).build())).build();
        Shop shop = Shop.builder().id(1L).name("오겹돼지").minOrderPrice(13_000L).build();

        CartLineItem existingLine = CartLineItem.builder().id(1L).menuId(1L).menuName("삼겹살 1인세트")
                .menuCount(1).basePrice(22_000L)
                .groups(new ArrayList<>(List.of(
                        CartOptionGroup.builder().id(1L).optionGroupId(1L).name("기본")
                                .options(new ArrayList<>(List.of(
                                        CartOption.builder().id(1L).optionId(1L).name("소(250g)").price(12_000L).build())))
                                .build())))
                .build();
        Cart cart = Cart.builder().id(1L).userId(1L).shopId(1L)
                .items(new ArrayList<>(List.of(existingLine))).build();

        given(cartRepository.findByUserId(1L)).willReturn(Optional.of(cart));
        given(cartRepository.save(any())).willAnswer(inv -> inv.getArgument(0));
        given(menuRepository.findById(1L)).willReturn(Optional.of(menu));
        given(menuRepository.findAllById(any())).willReturn(List.of(menu));
        given(optionGroupRepository.findAllById(any())).willReturn(List.of(og));
        given(shopService.loadShopOrThrow(1L)).willReturn(shop);

        SelectedOption sel = new SelectedOption(1L, "기본", 1L, "소(250g)", 12_000L);
        cartService.addItem(1L, new AddItemRequest("s", 1L, "삼겹살 1인세트", 3, List.of(sel)));

        verify(cartRepository).save(any(Cart.class));
        assertThat(cart.getItems()).hasSize(1);
        assertThat(cart.getItems().get(0).getMenuCount()).isEqualTo(1 + 3);
    }
}
