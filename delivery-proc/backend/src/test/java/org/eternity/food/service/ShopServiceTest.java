package org.eternity.food.service;

import org.eternity.food.Fixtures;
import org.eternity.food.dto.ShopDto;
import org.eternity.food.entity.Shop;
import org.eternity.food.repository.ShopRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;

import java.sql.Time;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("ShopService")
class ShopServiceTest {

    @Mock
    private ShopRepository shopRepository;

    @InjectMocks
    private ShopService shopService;

    @Nested
    @DisplayName("create() — name 불변식")
    class CreateNameInvariant {

        @Test
        @DisplayName("name이 null이면 IAE")
        void nameNull() {
            assertThatThrownBy(() -> shopService.create(
                    null,
                    13_000L,
                    LocalTime.of(9, 0),
                    LocalTime.of(22, 0),
                    "KOREAN",
                    37.5665,
                    126.9780,
                    3.0))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("가게 이름은 5글자 이상");
        }

        @ParameterizedTest(name = "name 길이 {0}글자(\"{0}\")면 IAE")
        @ValueSource(strings = {"", "가", "가게", "가게1", "가게12"})
        @DisplayName("name이 5글자 미만(경계 실패)이면 IAE")
        void nameTooShort(String name) {
            assertThatThrownBy(() -> shopService.create(
                    name,
                    13_000L,
                    LocalTime.of(9, 0),
                    LocalTime.of(22, 0),
                    "KOREAN",
                    37.5665,
                    126.9780,
                    3.0))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("가게 이름은 5글자 이상");
        }

        @Test
        @DisplayName("name이 정확히 5글자(경계 통과)면 생성된다")
        void nameBoundaryPass() {
            given(shopRepository.save(any(Shop.class))).willAnswer(inv -> inv.getArgument(0));

            Shop saved = shopService.create(
                    "오겹돼지집",
                    13_000L,
                    LocalTime.of(9, 0),
                    LocalTime.of(22, 0),
                    "KOREAN",
                    37.5665,
                    126.9780,
                    3.0);

            assertThat(saved.getName()).isEqualTo("오겹돼지집");
        }

        @Test
        @DisplayName("name이 충분히 긴 정상 값이면 생성된다")
        void nameNormal() {
            given(shopRepository.save(any(Shop.class))).willAnswer(inv -> inv.getArgument(0));

            Shop saved = shopService.create(
                    "매우긴가게이름입니다",
                    13_000L,
                    LocalTime.of(9, 0),
                    LocalTime.of(22, 0),
                    "KOREAN",
                    37.5665,
                    126.9780,
                    3.0);

            assertThat(saved.getName()).isEqualTo("매우긴가게이름입니다");
        }
    }

    @Nested
    @DisplayName("create() — minOrderPrice 불변식")
    class CreateMinOrderPriceInvariant {

        @Test
        @DisplayName("minOrderPrice가 null이면 IAE")
        void minOrderPriceNull() {
            assertThatThrownBy(() -> shopService.create(
                    "오겹돼지집",
                    null,
                    LocalTime.of(9, 0),
                    LocalTime.of(22, 0),
                    "KOREAN",
                    37.5665,
                    126.9780,
                    3.0))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("최소주문금액은 0원보다 커야");
        }

        @Test
        @DisplayName("minOrderPrice가 0원(경계 실패)이면 IAE")
        void minOrderPriceZero() {
            assertThatThrownBy(() -> shopService.create(
                    "오겹돼지집",
                    0L,
                    LocalTime.of(9, 0),
                    LocalTime.of(22, 0),
                    "KOREAN",
                    37.5665,
                    126.9780,
                    3.0))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("최소주문금액은 0원보다 커야");
        }

        @ParameterizedTest(name = "minOrderPrice={0}(음수)이면 IAE")
        @ValueSource(longs = {-1L, -100L, Long.MIN_VALUE})
        @DisplayName("minOrderPrice가 음수면 IAE")
        void minOrderPriceNegative(long price) {
            assertThatThrownBy(() -> shopService.create(
                    "오겹돼지집",
                    price,
                    LocalTime.of(9, 0),
                    LocalTime.of(22, 0),
                    "KOREAN",
                    37.5665,
                    126.9780,
                    3.0))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("최소주문금액은 0원보다 커야");
        }

        @Test
        @DisplayName("minOrderPrice가 1원(경계 통과)이면 생성된다")
        void minOrderPriceBoundaryPass() {
            given(shopRepository.save(any(Shop.class))).willAnswer(inv -> inv.getArgument(0));

            Shop saved = shopService.create(
                    "오겹돼지집",
                    1L,
                    LocalTime.of(9, 0),
                    LocalTime.of(22, 0),
                    "KOREAN",
                    37.5665,
                    126.9780,
                    3.0);

            assertThat(saved.getMinOrderPrice()).isEqualTo(1L);
        }

        @Test
        @DisplayName("minOrderPrice가 충분히 큰 정상 값이면 생성된다")
        void minOrderPriceNormal() {
            given(shopRepository.save(any(Shop.class))).willAnswer(inv -> inv.getArgument(0));

            Shop saved = shopService.create(
                    "오겹돼지집",
                    15_000L,
                    LocalTime.of(9, 0),
                    LocalTime.of(22, 0),
                    "KOREAN",
                    37.5665,
                    126.9780,
                    3.0);

            assertThat(saved.getMinOrderPrice()).isEqualTo(15_000L);
        }
    }

    @Nested
    @DisplayName("create() — operationPeriod 불변식")
    class CreateOperationPeriodInvariant {

        @ParameterizedTest
        @NullSource
        @DisplayName("startTime이 null이면 IAE")
        void startTimeNull(LocalTime startTime) {
            assertThatThrownBy(() -> shopService.create(
                    "오겹돼지집",
                    13_000L,
                    startTime,
                    LocalTime.of(22, 0),
                    "KOREAN",
                    37.5665,
                    126.9780,
                    3.0))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("영업시간은 null");
        }

        @ParameterizedTest
        @NullSource
        @DisplayName("endTime이 null이면 IAE")
        void endTimeNull(LocalTime endTime) {
            assertThatThrownBy(() -> shopService.create(
                    "오겹돼지집",
                    13_000L,
                    LocalTime.of(9, 0),
                    endTime,
                    "KOREAN",
                    37.5665,
                    126.9780,
                    3.0))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("영업시간은 null");
        }

        @Test
        @DisplayName("startTime/endTime 둘 다 null이면 IAE")
        void bothNull() {
            assertThatThrownBy(() -> shopService.create(
                    "오겹돼지집",
                    13_000L,
                    null,
                    null,
                    "KOREAN",
                    37.5665,
                    126.9780,
                    3.0))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("영업시간은 null");
        }

        @Test
        @DisplayName("startTime == endTime(경계 실패)이면 IAE")
        void startEqualsEnd() {
            assertThatThrownBy(() -> shopService.create(
                    "오겹돼지집",
                    13_000L,
                    LocalTime.of(9, 0),
                    LocalTime.of(9, 0),
                    "KOREAN",
                    37.5665,
                    126.9780,
                    3.0))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("영업 시작 시각은 종료 시각보다 빨라야");
        }

        @Test
        @DisplayName("startTime > endTime이면 IAE")
        void startAfterEnd() {
            assertThatThrownBy(() -> shopService.create(
                    "오겹돼지집",
                    13_000L,
                    LocalTime.of(22, 0),
                    LocalTime.of(9, 0),
                    "KOREAN",
                    37.5665,
                    126.9780,
                    3.0))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("영업 시작 시각은 종료 시각보다 빨라야");
        }

        @Test
        @DisplayName("startTime < endTime 1분 차이(경계 통과)면 생성된다")
        void startBeforeEndByOneMinute() {
            given(shopRepository.save(any(Shop.class))).willAnswer(inv -> inv.getArgument(0));

            Shop saved = shopService.create(
                    "오겹돼지집",
                    13_000L,
                    LocalTime.of(9, 0),
                    LocalTime.of(9, 1),
                    "KOREAN",
                    37.5665,
                    126.9780,
                    3.0);

            assertThat(saved.getStartTime()).isEqualTo(LocalTime.of(9, 0));
            assertThat(saved.getEndTime()).isEqualTo(LocalTime.of(9, 1));
        }

        @Test
        @DisplayName("startTime/endTime 정상 값이면 생성된다")
        void operationPeriodNormal() {
            given(shopRepository.save(any(Shop.class))).willAnswer(inv -> inv.getArgument(0));

            Shop saved = shopService.create(
                    "오겹돼지집",
                    13_000L,
                    LocalTime.of(10, 0),
                    LocalTime.of(20, 0),
                    "KOREAN",
                    37.5665,
                    126.9780,
                    3.0);

            assertThat(saved.getStartTime()).isEqualTo(LocalTime.of(10, 0));
            assertThat(saved.getEndTime()).isEqualTo(LocalTime.of(20, 0));
        }
    }

    @Nested
    @DisplayName("create() — save 위임")
    class CreateSaveDelegation {

        @Test
        @DisplayName("모든 invariant 통과 시 ShopRepository.save가 정확한 Shop으로 호출된다")
        void savesShopWithAllFields() {
            given(shopRepository.save(any(Shop.class))).willAnswer(inv -> inv.getArgument(0));

            shopService.create(
                    "오겹돼지집",
                    13_000L,
                    LocalTime.of(9, 0),
                    LocalTime.of(22, 0),
                    "KOREAN",
                    37.5665,
                    126.9780,
                    3.0);

            ArgumentCaptor<Shop> captor = ArgumentCaptor.forClass(Shop.class);
            verify(shopRepository).save(captor.capture());
            Shop captured = captor.getValue();
            assertThat(captured.getName()).isEqualTo("오겹돼지집");
            assertThat(captured.getMinOrderPrice()).isEqualTo(13_000L);
            assertThat(captured.getStartTime()).isEqualTo(LocalTime.of(9, 0));
            assertThat(captured.getEndTime()).isEqualTo(LocalTime.of(22, 0));
            assertThat(captured.getCategory()).isEqualTo("KOREAN");
            assertThat(captured.getLatitude()).isEqualTo(37.5665);
            assertThat(captured.getLongitude()).isEqualTo(126.9780);
            assertThat(captured.getDeliveryRadius()).isEqualTo(3.0);
        }

        @Test
        @DisplayName("validateShop 실패 시 save는 호출되지 않는다")
        void doesNotSaveOnValidationFailure() {
            assertThatThrownBy(() -> shopService.create(
                    null,
                    13_000L,
                    LocalTime.of(9, 0),
                    LocalTime.of(22, 0),
                    "KOREAN",
                    37.5665,
                    126.9780,
                    3.0))
                    .isInstanceOf(IllegalArgumentException.class);

            verify(shopRepository, org.mockito.Mockito.never()).save(any(Shop.class));
        }
    }

    @Nested
    @DisplayName("create() — happy path")
    class CreateHappyPath {

        @Test
        @DisplayName("모든 invariant를 통과하면 정상 생성되어 반환된다")
        void allFieldsValid() {
            given(shopRepository.save(any(Shop.class))).willAnswer(inv -> {
                Shop s = inv.getArgument(0);
                s.setId(99L);
                return s;
            });

            assertThatCode(() -> {
                Shop saved = shopService.create(
                        "오겹돼지집",
                        13_000L,
                        LocalTime.of(9, 0),
                        LocalTime.of(22, 0),
                        "KOREAN",
                        37.5665,
                        126.9780,
                        3.0);
                assertThat(saved.getId()).isEqualTo(99L);
            }).doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("findById()")
    class FindById {

        @Test
        @DisplayName("shop이 존재하면 Detail DTO를 반환한다")
        void shopExists() {
            Shop shop = Fixtures.aShop().build();
            given(shopRepository.findById(Fixtures.SHOP_ID)).willReturn(Optional.of(shop));

            ShopDto.Detail detail = shopService.findById(Fixtures.SHOP_ID);

            assertThat(detail.id()).isEqualTo(shop.getId());
            assertThat(detail.name()).isEqualTo(shop.getName());
            assertThat(detail.category()).isEqualTo("한식");
            assertThat(detail.latitude()).isEqualTo(shop.getLatitude());
            assertThat(detail.longitude()).isEqualTo(shop.getLongitude());
            assertThat(detail.minOrderAmount()).isEqualTo(shop.getMinOrderPrice());
        }

        @Test
        @DisplayName("shop이 존재하지 않으면 IAE")
        void shopNotFound() {
            given(shopRepository.findById(999L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> shopService.findById(999L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("가게를 찾을 수 없습니다")
                    .hasMessageContaining("999");
        }
    }

    @Nested
    @DisplayName("loadShopOrThrow()")
    class LoadShopOrThrow {

        @Test
        @DisplayName("shop이 존재하면 Shop entity를 반환한다")
        void shopExists() {
            Shop shop = Fixtures.aShop().build();
            given(shopRepository.findById(Fixtures.SHOP_ID)).willReturn(Optional.of(shop));

            Shop result = shopService.loadShopOrThrow(Fixtures.SHOP_ID);

            assertThat(result).isSameAs(shop);
        }

        @Test
        @DisplayName("shop이 존재하지 않으면 IAE")
        void shopNotFound() {
            given(shopRepository.findById(999L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> shopService.loadShopOrThrow(999L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("가게를 찾을 수 없습니다")
                    .hasMessageContaining("999");
        }
    }

    @Nested
    @DisplayName("isShopOpen()")
    class IsShopOpen {

        @Test
        @DisplayName("현재 시각이 영업시간 안에 있으면 true")
        void withinOperatingHours() {
            Shop shop = Fixtures.aShop()
                    .startTime(LocalTime.of(0, 0))
                    .endTime(LocalTime.of(23, 59))
                    .build();

            assertThat(shopService.isShopOpen(shop)).isTrue();
        }

        @Test
        @DisplayName("현재 시각이 영업시간 종료 이후이면 false")
        void afterClose() {
            LocalTime now = LocalTime.now();
            if (now.isBefore(LocalTime.of(0, 2))) {
                return;
            }

            LocalTime end = now.minusMinutes(1);
            LocalTime start = end.minusHours(1).isBefore(end) ? end.minusHours(1) : LocalTime.of(0, 0);
            Shop shop = Fixtures.aShop()
                    .startTime(start)
                    .endTime(end)
                    .build();

            assertThat(shopService.isShopOpen(shop)).isFalse();
        }

        @Test
        @DisplayName("현재 시각이 영업 시작 이전이면 false")
        void beforeOpen() {
            LocalTime now = LocalTime.now();
            if (!now.isBefore(LocalTime.of(23, 58))) {
                return;
            }

            LocalTime start = now.plusMinutes(1);
            LocalTime end = start.plusMinutes(1);
            Shop shop = Fixtures.aShop()
                    .startTime(start)
                    .endTime(end)
                    .build();

            assertThat(shopService.isShopOpen(shop)).isFalse();
        }

        @Test
        @DisplayName("startTime이 null이면 false")
        void startTimeNull() {
            Shop shop = Fixtures.aShop().startTime(null).build();

            assertThat(shopService.isShopOpen(shop)).isFalse();
        }

        @Test
        @DisplayName("endTime이 null이면 false")
        void endTimeNull() {
            Shop shop = Fixtures.aShop().endTime(null).build();

            assertThat(shopService.isShopOpen(shop)).isFalse();
        }

        @Test
        @DisplayName("startTime/endTime 둘 다 null이면 false")
        void bothNull() {
            Shop shop = Fixtures.aShop().startTime(null).endTime(null).build();

            assertThat(shopService.isShopOpen(shop)).isFalse();
        }

        @Test
        @DisplayName("startTime == endTime이면 false (영업시간 0분)")
        void startEqualsEnd() {
            Shop shop = Fixtures.aShop()
                    .startTime(LocalTime.of(9, 0))
                    .endTime(LocalTime.of(9, 0))
                    .build();

            assertThat(shopService.isShopOpen(shop)).isFalse();
        }

        @Test
        @DisplayName("startTime > endTime(역전)이면 false")
        void startAfterEnd() {
            Shop shop = Fixtures.aShop()
                    .startTime(LocalTime.of(22, 0))
                    .endTime(LocalTime.of(9, 0))
                    .build();

            assertThat(shopService.isShopOpen(shop)).isFalse();
        }
    }

    @Nested
    @DisplayName("findNearby()")
    class FindNearby {

        private Pageable pageable() {
            return PageRequest.of(0, 10);
        }

        private Object[] sampleRow(Long id, String name, String categoryCode, double distance) {
            return new Object[]{
                    id,
                    name,
                    13_000L,
                    Time.valueOf(LocalTime.of(0, 0)),
                    Time.valueOf(LocalTime.of(23, 59)),
                    categoryCode,
                    37.5665,
                    126.9780,
                    3.0,
                    distance
            };
        }

        @Test
        @DisplayName("정상 row 1개를 Nearby DTO 1개로 매핑한다")
        void mapsRowsToDto() {
            List<Object[]> content = new java.util.ArrayList<>();
            content.add(sampleRow(1L, "오겹돼지집", "KOREAN", 1.2));
            Slice<Object[]> rows = new SliceImpl<>(content, pageable(), false);
            given(shopRepository.findNearbyWithDistance(
                    org.mockito.ArgumentMatchers.anyDouble(), org.mockito.ArgumentMatchers.anyDouble(),
                    org.mockito.ArgumentMatchers.any(),
                    eq(pageable()))).willReturn(rows);

            Slice<ShopDto.Nearby> result = shopService.findNearby(37.5665, 126.9780, null, pageable());

            assertThat(result.getContent()).hasSize(1);
            ShopDto.Nearby dto = result.getContent().get(0);
            assertThat(dto.id()).isEqualTo(1L);
            assertThat(dto.name()).isEqualTo("오겹돼지집");
            assertThat(dto.category()).isEqualTo("한식");
            assertThat(dto.latitude()).isEqualTo(37.5665);
            assertThat(dto.longitude()).isEqualTo(126.9780);
            assertThat(dto.minOrderAmount()).isEqualTo(13_000L);
            assertThat(dto.distance()).isEqualTo(1.2);
            assertThat(dto.open()).isTrue();
        }

        @Test
        @DisplayName("category가 null이면 categoryCode=null로 repository 호출")
        void categoryNullPassesNull() {
            given(shopRepository.findNearbyWithDistance(
                    org.mockito.ArgumentMatchers.anyDouble(), org.mockito.ArgumentMatchers.anyDouble(),
                    isNull(),
                    eq(pageable()))).willReturn(new SliceImpl<>(Collections.emptyList(), pageable(), false));

            shopService.findNearby(37.5665, 126.9780, null, pageable());

            verify(shopRepository).findNearbyWithDistance(
                    org.mockito.ArgumentMatchers.anyDouble(), org.mockito.ArgumentMatchers.anyDouble(),
                    isNull(),
                    eq(pageable()));
        }

        @Test
        @DisplayName("category가 빈 문자열이면 categoryCode=null로 repository 호출")
        void categoryBlankPassesNull() {
            given(shopRepository.findNearbyWithDistance(
                    org.mockito.ArgumentMatchers.anyDouble(), org.mockito.ArgumentMatchers.anyDouble(),
                    isNull(),
                    eq(pageable()))).willReturn(new SliceImpl<>(Collections.emptyList(), pageable(), false));

            shopService.findNearby(37.5665, 126.9780, "  ", pageable());

            verify(shopRepository).findNearbyWithDistance(
                    org.mockito.ArgumentMatchers.anyDouble(), org.mockito.ArgumentMatchers.anyDouble(),
                    isNull(),
                    eq(pageable()));
        }

        @Test
        @DisplayName("category가 \"전체\"이면 categoryCode=null로 repository 호출")
        void categoryAllPassesNull() {
            given(shopRepository.findNearbyWithDistance(
                    org.mockito.ArgumentMatchers.anyDouble(), org.mockito.ArgumentMatchers.anyDouble(),
                    isNull(),
                    eq(pageable()))).willReturn(new SliceImpl<>(Collections.emptyList(), pageable(), false));

            shopService.findNearby(37.5665, 126.9780, "전체", pageable());

            verify(shopRepository).findNearbyWithDistance(
                    org.mockito.ArgumentMatchers.anyDouble(), org.mockito.ArgumentMatchers.anyDouble(),
                    isNull(),
                    eq(pageable()));
        }

        @Test
        @DisplayName("category가 한식이면 KOREAN 코드로 변환되어 repository 호출")
        void categoryKoreanConverted() {
            given(shopRepository.findNearbyWithDistance(
                    org.mockito.ArgumentMatchers.anyDouble(), org.mockito.ArgumentMatchers.anyDouble(),
                    eq("KOREAN"),
                    eq(pageable()))).willReturn(new SliceImpl<>(Collections.emptyList(), pageable(), false));

            shopService.findNearby(37.5665, 126.9780, "한식", pageable());

            verify(shopRepository).findNearbyWithDistance(
                    org.mockito.ArgumentMatchers.anyDouble(), org.mockito.ArgumentMatchers.anyDouble(),
                    eq("KOREAN"),
                    eq(pageable()));
        }

        @Test
        @DisplayName("결과가 비어있으면 빈 Slice를 반환")
        void emptyResult() {
            given(shopRepository.findNearbyWithDistance(
                    org.mockito.ArgumentMatchers.anyDouble(), org.mockito.ArgumentMatchers.anyDouble(),
                    org.mockito.ArgumentMatchers.any(),
                    eq(pageable()))).willReturn(new SliceImpl<>(Collections.emptyList(), pageable(), false));

            Slice<ShopDto.Nearby> result = shopService.findNearby(37.5665, 126.9780, null, pageable());

            assertThat(result.getContent()).isEmpty();
            assertThat(result.hasNext()).isFalse();
        }

        @Test
        @DisplayName("row의 latitude/longitude가 null이면 DTO에도 null로 매핑된다 (L118-119 null guard)")
        void mapsNullCoordinates() {
            Object[] row = new Object[]{
                    1L,
                    "오겹돼지집",
                    13_000L,
                    Time.valueOf(LocalTime.of(0, 0)),
                    Time.valueOf(LocalTime.of(23, 59)),
                    "KOREAN",
                    null,
                    null,
                    3.0,
                    2.5
            };
            List<Object[]> content = new java.util.ArrayList<>();
            content.add(row);
            Slice<Object[]> rows = new SliceImpl<>(content, pageable(), false);
            given(shopRepository.findNearbyWithDistance(
                    org.mockito.ArgumentMatchers.anyDouble(), org.mockito.ArgumentMatchers.anyDouble(),
                    org.mockito.ArgumentMatchers.any(),
                    eq(pageable()))).willReturn(rows);

            Slice<ShopDto.Nearby> result = shopService.findNearby(37.5665, 126.9780, null, pageable());

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).latitude()).isNull();
            assertThat(result.getContent().get(0).longitude()).isNull();
            assertThat(result.getContent().get(0).distance()).isEqualTo(2.5);
        }

        @Test
        @DisplayName("hasNext=true가 전파된다")
        void hasNextPropagated() {
            List<Object[]> content = new java.util.ArrayList<>();
            content.add(sampleRow(1L, "오겹돼지집", "KOREAN", 0.5));
            Slice<Object[]> rows = new SliceImpl<>(content, pageable(), true);
            given(shopRepository.findNearbyWithDistance(
                    org.mockito.ArgumentMatchers.anyDouble(), org.mockito.ArgumentMatchers.anyDouble(),
                    org.mockito.ArgumentMatchers.any(),
                    eq(pageable()))).willReturn(rows);

            Slice<ShopDto.Nearby> result = shopService.findNearby(37.5665, 126.9780, null, pageable());

            assertThat(result.hasNext()).isTrue();
        }
    }
}
