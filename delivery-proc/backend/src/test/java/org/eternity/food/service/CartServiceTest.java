package org.eternity.food.service;

import org.eternity.food.dto.CartDto.AddItemRequest;
import org.eternity.food.entity.Cart;
import org.eternity.food.entity.CartLineItem;
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
    @DisplayName("다중 라인 — 라인별 unitPrice × count 합산")
    void multipleLines_sumsCorrectly() {
        // 라인1: 22,000 × 1 = 22,000
        CartLineItem line1 = CartLineItem.builder().menuId(1L).unitPrice(22_000L).menuCount(1).build();
        // 라인2: 23,000 × 2 = 46,000
        CartLineItem line2 = CartLineItem.builder().menuId(2L).unitPrice(23_000L).menuCount(2).build();
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
                        CartLineItem.builder().menuId(77L).menuCount(1).unitPrice(0L).build())))
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
}
