package org.eternity.food.service;

import org.eternity.food.Fixtures;
import org.eternity.food.dto.OrderDto;
import org.eternity.food.entity.Order;
import org.eternity.food.entity.OrderLineItem;
import org.eternity.food.entity.Shop;
import org.eternity.food.repository.OrderRepository;
import org.eternity.food.repository.ShopRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ShopRepository shopRepository;

    @InjectMocks
    private OrderService orderService;

    private Pageable pageable;

    @BeforeEach
    void setUp() {
        pageable = PageRequest.of(0, 10);
    }

    @Test
    @DisplayName("빈 결과면 empty Page 반환 + shopRepository 호출 안 함")
    void findOrders_empty_returnsEmptyPage() {
        Page<Order> empty = new PageImpl<>(List.of(), pageable, 0L);
        when(orderRepository.findByUserId(eq(Fixtures.USER_ID), eq(pageable))).thenReturn(empty);

        Page<OrderDto.OrderResponse> result = orderService.findOrders(Fixtures.USER_ID, pageable);

        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isZero();
        verify(shopRepository, never()).findAllById(any());
    }

    @Test
    @DisplayName("정상: order entity → OrderResponse 변환 + 페이징 메타 전달")
    void findOrders_singleOrder_mapsAllFields() {
        OrderLineItem.OrderOption opt = Fixtures.anOrderOption()
                .name("소(250g)")
                .price(12_000L)
                .build();
        OrderLineItem.OrderOptionGroup group = Fixtures.anOrderOptionGroup()
                .name("기본")
                .options(List.of(opt))
                .build();
        OrderLineItem line = Fixtures.anOrderLineItem()
                .menuName("삼겹살 1인세트")
                .count(2)
                .unitPrice(10_000L)
                .groups(List.of(group))
                .build();
        Order order = Fixtures.anOrder()
                .totalPrice(22_000L)
                .orderedTime(LocalDateTime.of(2024, 1, 1, 12, 0))
                .itemsSnapshot(new ArrayList<>(List.of(line)))
                .build();
        Page<Order> page = new PageImpl<>(List.of(order), pageable, 1L);
        when(orderRepository.findByUserId(eq(Fixtures.USER_ID), eq(pageable))).thenReturn(page);

        Shop shop = Fixtures.aShop().name("오겹돼지").build();
        when(shopRepository.findAllById(any())).thenReturn(List.of(shop));

        Page<OrderDto.OrderResponse> result = orderService.findOrders(Fixtures.USER_ID, pageable);

        assertThat(result.getTotalElements()).isEqualTo(1L);
        assertThat(result.getContent()).hasSize(1);

        OrderDto.OrderResponse resp = result.getContent().get(0);
        assertThat(resp.id()).isEqualTo(Fixtures.ORDER_ID);
        assertThat(resp.shopId()).isEqualTo(Fixtures.SHOP_ID);
        assertThat(resp.shopName()).isEqualTo("오겹돼지");
        assertThat(resp.orderedTime()).isEqualTo(LocalDateTime.of(2024, 1, 1, 12, 0));
        assertThat(resp.totalPrice()).isEqualTo(22_000L);

        assertThat(resp.items()).hasSize(1);
        OrderDto.LineItem li = resp.items().get(0);
        assertThat(li.menuName()).isEqualTo("삼겹살 1인세트");
        assertThat(li.quantity()).isEqualTo(2);
        assertThat(li.unitPrice()).isEqualTo(10_000L);
        assertThat(li.subtotal()).isEqualTo(20_000L);

        assertThat(li.options()).hasSize(1);
        OrderDto.Option o = li.options().get(0);
        assertThat(o.groupName()).isEqualTo("기본");
        assertThat(o.name()).isEqualTo("소(250g)");
        assertThat(o.price()).isEqualTo(12_000L);
    }

    @Test
    @DisplayName("여러 주문: shopId 모아서 한 번에 findAllById 호출 + 중복 제거")
    void findOrders_multipleOrders_batchesShopLookup() {
        Long shopId2 = 2L;
        Order order1 = Fixtures.anOrder().id(1L).shopId(Fixtures.SHOP_ID).build();
        Order order2 = Fixtures.anOrder().id(2L).shopId(shopId2).build();
        Order order3 = Fixtures.anOrder().id(3L).shopId(Fixtures.SHOP_ID).build();
        Page<Order> page = new PageImpl<>(List.of(order1, order2, order3), pageable, 3L);
        when(orderRepository.findByUserId(eq(Fixtures.USER_ID), eq(pageable))).thenReturn(page);

        Shop shop1 = Fixtures.aShop().id(Fixtures.SHOP_ID).name("오겹돼지").build();
        Shop shop2 = Fixtures.aShop().id(shopId2).name("초밥집").build();
        when(shopRepository.findAllById(any())).thenReturn(List.of(shop1, shop2));

        Page<OrderDto.OrderResponse> result = orderService.findOrders(Fixtures.USER_ID, pageable);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Iterable<Long>> captor = ArgumentCaptor.forClass(Iterable.class);
        verify(shopRepository).findAllById(captor.capture());
        List<Long> requestedIds = new ArrayList<>();
        captor.getValue().forEach(requestedIds::add);
        assertThat(requestedIds).containsExactlyInAnyOrder(Fixtures.SHOP_ID, shopId2);

        assertThat(result.getContent()).hasSize(3);
        assertThat(result.getContent().get(0).shopName()).isEqualTo("오겹돼지");
        assertThat(result.getContent().get(1).shopName()).isEqualTo("초밥집");
        assertThat(result.getContent().get(2).shopName()).isEqualTo("오겹돼지");
    }

    @Test
    @DisplayName("itemsSnapshot null → 빈 items 리스트")
    void findOrders_nullSnapshot_emptyItems() {
        Order order = Fixtures.anOrder().itemsSnapshot(null).build();
        Page<Order> page = new PageImpl<>(List.of(order), pageable, 1L);
        when(orderRepository.findByUserId(eq(Fixtures.USER_ID), eq(pageable))).thenReturn(page);
        when(shopRepository.findAllById(any())).thenReturn(List.of(Fixtures.aShop().build()));

        Page<OrderDto.OrderResponse> result = orderService.findOrders(Fixtures.USER_ID, pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).items()).isEmpty();
    }

    @Test
    @DisplayName("LineItem의 groups null → 빈 options 리스트")
    void findOrders_lineItemWithNullGroups_emptyOptions() {
        OrderLineItem line = Fixtures.anOrderLineItem()
                .count(1)
                .unitPrice(10_000L)
                .groups(null)
                .build();
        Order order = Fixtures.anOrder()
                .itemsSnapshot(new ArrayList<>(List.of(line)))
                .build();
        Page<Order> page = new PageImpl<>(List.of(order), pageable, 1L);
        when(orderRepository.findByUserId(eq(Fixtures.USER_ID), eq(pageable))).thenReturn(page);
        when(shopRepository.findAllById(any())).thenReturn(List.of(Fixtures.aShop().build()));

        Page<OrderDto.OrderResponse> result = orderService.findOrders(Fixtures.USER_ID, pageable);

        OrderDto.LineItem li = result.getContent().get(0).items().get(0);
        assertThat(li.options()).isEmpty();
    }

    @Test
    @DisplayName("OptionGroup의 options null → 해당 그룹 skip (continue branch)")
    void findOrders_optionGroupWithNullOptions_skipsGroup() {
        OrderLineItem.OrderOptionGroup goodGroup = Fixtures.anOrderOptionGroup()
                .name("기본")
                .options(List.of(Fixtures.anOrderOption().name("소").price(1_000L).build()))
                .build();
        OrderLineItem.OrderOptionGroup nullGroup = Fixtures.anOrderOptionGroup()
                .name("추가")
                .options(null)
                .build();
        OrderLineItem line = Fixtures.anOrderLineItem()
                .count(1)
                .unitPrice(10_000L)
                .groups(List.of(goodGroup, nullGroup))
                .build();
        Order order = Fixtures.anOrder()
                .itemsSnapshot(new ArrayList<>(List.of(line)))
                .build();
        Page<Order> page = new PageImpl<>(List.of(order), pageable, 1L);
        when(orderRepository.findByUserId(eq(Fixtures.USER_ID), eq(pageable))).thenReturn(page);
        when(shopRepository.findAllById(any())).thenReturn(List.of(Fixtures.aShop().build()));

        Page<OrderDto.OrderResponse> result = orderService.findOrders(Fixtures.USER_ID, pageable);

        OrderDto.LineItem li = result.getContent().get(0).items().get(0);
        assertThat(li.options()).hasSize(1);
        assertThat(li.options().get(0).groupName()).isEqualTo("기본");
    }

    @Test
    @DisplayName("LineItem의 count/unitPrice null → 0 / 0L defaulting, subtotal=0")
    void findOrders_lineItemWithNullCountAndUnitPrice_defaultsToZero() {
        OrderLineItem line = Fixtures.anOrderLineItem()
                .count(null)
                .unitPrice(null)
                .groups(List.of())
                .build();
        Order order = Fixtures.anOrder()
                .itemsSnapshot(new ArrayList<>(List.of(line)))
                .build();
        Page<Order> page = new PageImpl<>(List.of(order), pageable, 1L);
        when(orderRepository.findByUserId(eq(Fixtures.USER_ID), eq(pageable))).thenReturn(page);
        when(shopRepository.findAllById(any())).thenReturn(List.of(Fixtures.aShop().build()));

        Page<OrderDto.OrderResponse> result = orderService.findOrders(Fixtures.USER_ID, pageable);

        OrderDto.LineItem li = result.getContent().get(0).items().get(0);
        assertThat(li.quantity()).isZero();
        assertThat(li.unitPrice()).isZero();
        assertThat(li.subtotal()).isZero();
    }

    @Test
    @DisplayName("Option의 price null → 0L defaulting")
    void findOrders_optionWithNullPrice_defaultsToZero() {
        OrderLineItem.OrderOption opt = Fixtures.anOrderOption()
                .name("소")
                .price(null)
                .build();
        OrderLineItem.OrderOptionGroup group = Fixtures.anOrderOptionGroup()
                .name("기본")
                .options(List.of(opt))
                .build();
        OrderLineItem line = Fixtures.anOrderLineItem()
                .count(1)
                .unitPrice(10_000L)
                .groups(List.of(group))
                .build();
        Order order = Fixtures.anOrder()
                .itemsSnapshot(new ArrayList<>(List.of(line)))
                .build();
        Page<Order> page = new PageImpl<>(List.of(order), pageable, 1L);
        when(orderRepository.findByUserId(eq(Fixtures.USER_ID), eq(pageable))).thenReturn(page);
        when(shopRepository.findAllById(any())).thenReturn(List.of(Fixtures.aShop().build()));

        Page<OrderDto.OrderResponse> result = orderService.findOrders(Fixtures.USER_ID, pageable);

        OrderDto.Option o = result.getContent().get(0).items().get(0).options().get(0);
        assertThat(o.price()).isZero();
    }

    @Test
    @DisplayName("Order의 totalPrice null → 0L defaulting")
    void findOrders_orderWithNullTotalPrice_defaultsToZero() {
        Order order = Fixtures.anOrder()
                .totalPrice(null)
                .itemsSnapshot(new ArrayList<>())
                .build();
        Page<Order> page = new PageImpl<>(List.of(order), pageable, 1L);
        when(orderRepository.findByUserId(eq(Fixtures.USER_ID), eq(pageable))).thenReturn(page);
        when(shopRepository.findAllById(any())).thenReturn(List.of(Fixtures.aShop().build()));

        Page<OrderDto.OrderResponse> result = orderService.findOrders(Fixtures.USER_ID, pageable);

        assertThat(result.getContent().get(0).totalPrice()).isZero();
    }

    @Test
    @DisplayName("shopRepository에 매칭되는 shop이 없으면 shopName=null (map lookup miss)")
    void findOrders_missingShop_shopNameIsNull() {
        Order order = Fixtures.anOrder()
                .itemsSnapshot(new ArrayList<>())
                .build();
        Page<Order> page = new PageImpl<>(List.of(order), pageable, 1L);
        when(orderRepository.findByUserId(eq(Fixtures.USER_ID), eq(pageable))).thenReturn(page);
        when(shopRepository.findAllById(any())).thenReturn(List.of());

        Page<OrderDto.OrderResponse> result = orderService.findOrders(Fixtures.USER_ID, pageable);

        assertThat(result.getContent().get(0).shopName()).isNull();
    }

    @Test
    @DisplayName("userId null: service가 검증하지 않고 그대로 repository에 위임 (현재 spec)")
    void findOrders_nullUserId_delegatesToRepositoryAsIs() {
        Page<Order> empty = new PageImpl<>(List.of(), pageable, 0L);
        when(orderRepository.findByUserId(eq(null), eq(pageable))).thenReturn(empty);

        Page<OrderDto.OrderResponse> result = orderService.findOrders(null, pageable);

        assertThat(result.getContent()).isEmpty();
        verify(orderRepository).findByUserId(eq(null), eq(pageable));
    }
}
