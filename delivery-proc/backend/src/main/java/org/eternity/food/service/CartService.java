package org.eternity.food.service;

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
import org.eternity.food.entity.OrderLineItem;
import org.eternity.food.entity.Shop;
import org.eternity.food.repository.CartRepository;
import org.eternity.food.repository.MenuRepository;
import org.eternity.food.repository.OptionGroupRepository;
import org.eternity.food.repository.OrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 장바구니 + 주문전환 fat-service. 절차지향 anti-pattern을 의도적으로 응집해 둔 클래스.
 *
 * <p>이 한 클래스에:
 * <ul>
 *   <li>장바구니 조회 + 카탈로그 비교 reconciliation (200줄짜리 getCart)</li>
 *   <li>아이템 추가/수정/삭제 (mutation은 entity setter 체이닝)</li>
 *   <li>주문 전환 검증 + Order 저장 + cart 비우기</li>
 * </ul>
 *
 * <p>같은 메뉴 카탈로그를 여러 메서드에서 반복해서 로드한다. 캐싱 없음. 중복 검증 흩어짐.
 */
@Service
public class CartService {

    private static final String MENU_STATUS_OPEN = "OPEN";

    private final CartRepository cartRepository;
    private final MenuRepository menuRepository;
    private final OptionGroupRepository optionGroupRepository;
    private final OrderRepository orderRepository;
    private final ShopService shopService;

    public CartService(CartRepository cartRepository,
                       MenuRepository menuRepository,
                       OptionGroupRepository optionGroupRepository,
                       OrderRepository orderRepository,
                       ShopService shopService) {
        this.cartRepository = cartRepository;
        this.menuRepository = menuRepository;
        this.optionGroupRepository = optionGroupRepository;
        this.orderRepository = orderRepository;
        this.shopService = shopService;
    }

    // ====================================================================
    // 카트 조회 + reconciliation
    // (이 한 메서드가 200줄 가까이 됩니다. 의도된 fat-service 데모입니다.)
    // ====================================================================
    @Transactional(readOnly = true)
    public CartDto.CartResponse getCart(Long userId, String sessionId) {
        // 1. cart 헤더 조회
        Cart cart = cartRepository.findByUserId(userId).orElse(null);
        if (cart == null) {
            return CartDto.CartResponse.empty(sessionId);
        }

        // 2. 카트 라인이 비어있는 경우
        if (cart.getItems().isEmpty()) {
            return new CartDto.CartResponse(
                    cart.getId(),
                    sessionId,
                    cart.getShopId() == null ? null : loadShopBrief(cart.getShopId()),
                    List.of(),
                    0L);
        }

        // 3. 현재 카트의 모든 menuId 모아서 카탈로그 한 번에 로드
        Set<Long> menuIds = new HashSet<>();
        for (CartLineItem item : cart.getItems()) {
            menuIds.add(item.getMenuId());
        }
        Map<Long, Menu> currentMenusById = new HashMap<>();
        for (Menu menu : menuRepository.findAllById(menuIds)) {
            currentMenusById.put(menu.getId(), menu);
        }

        // 4. 메뉴들이 참조하는 option group들 모두 로드
        Set<Long> ogIds = new HashSet<>();
        for (Menu menu : currentMenusById.values()) {
            for (MenuOptionGroup mog : menu.getOptionGroups()) {
                ogIds.add(mog.getOptionGroupId());
            }
        }
        Map<Long, OptionGroup> currentOgsById = new HashMap<>();
        if (!ogIds.isEmpty()) {
            for (OptionGroup og : optionGroupRepository.findAllById(ogIds)) {
                currentOgsById.put(og.getId(), og);
            }
        }

        // 5. 각 라인 reconcile (이 안쪽이 또 100줄)
        List<CartDto.Item> resultItems = new ArrayList<>();
        long totalPrice = 0L;
        for (CartLineItem line : cart.getItems()) {
            Menu menu = currentMenusById.get(line.getMenuId());
            List<String> messages = new ArrayList<>();

            // 5-A. 메뉴 자체가 사라졌을 때
            if (menu == null) {
                List<CartDto.Option> opts = new ArrayList<>();
                for (CartOptionGroup g : line.getGroups()) {
                    for (CartOption co : g.getOptions()) {
                        opts.add(new CartDto.Option(
                                g.getName(), co.getName(), co.getPrice(), CartDto.OptionStatus.INVALID));
                    }
                }
                resultItems.add(new CartDto.Item(
                        line.getId(),
                        line.getMenuId(),
                        line.getMenuName(),
                        line.getUnitPrice(),
                        line.getMenuCount(),
                        opts,
                        CartDto.ItemStatus.MENU_REMOVED,
                        List.of("이 메뉴는 더 이상 판매되지 않습니다.")
                ));
                totalPrice += line.getUnitPrice() * line.getMenuCount();
                continue;
            }

            // 5-B. 메뉴 이름 변동 메시지
            if (!menu.getName().equals(line.getMenuName())) {
                messages.add(String.format("메뉴 이름이 변경되었습니다: %s → %s",
                        line.getMenuName(), menu.getName()));
            }

            // 5-C. 옵션 reconcile
            Set<Long> validGroupIds = new HashSet<>();
            for (MenuOptionGroup mog : menu.getOptionGroups()) {
                validGroupIds.add(mog.getOptionGroupId());
            }

            boolean anyOptionInvalid = false;
            boolean anyPriceChanged = false;
            long unitTotal = menu.getBasePrice();
            List<CartDto.Option> reconciledOptions = new ArrayList<>();

            for (CartOptionGroup cartGroup : line.getGroups()) {
                OptionGroup currentGroup = currentOgsById.get(cartGroup.getOptionGroupId());

                // 그룹이 없어졌거나 더 이상 이 메뉴에 속하지 않음
                if (currentGroup == null || !validGroupIds.contains(currentGroup.getId())) {
                    for (CartOption co : cartGroup.getOptions()) {
                        reconciledOptions.add(new CartDto.Option(
                                cartGroup.getName(),
                                co.getName(),
                                co.getPrice(),
                                CartDto.OptionStatus.INVALID
                        ));
                        unitTotal += co.getPrice();
                    }
                    anyOptionInvalid = true;
                    continue;
                }

                String displayGroupName = currentGroup.getName();
                if (!displayGroupName.equals(cartGroup.getName())) {
                    messages.add(String.format("옵션 그룹 이름이 변경되었습니다: %s → %s",
                            cartGroup.getName(), displayGroupName));
                }

                // option id → Option맵
                Map<Long, Option> optById = new HashMap<>();
                for (Option o : currentGroup.getOptions()) {
                    optById.put(o.getId(), o);
                }

                for (CartOption cartOption : cartGroup.getOptions()) {
                    Option current = optById.get(cartOption.getOptionId());
                    if (current == null) {
                        reconciledOptions.add(new CartDto.Option(
                                displayGroupName,
                                cartOption.getName(),
                                cartOption.getPrice(),
                                CartDto.OptionStatus.INVALID
                        ));
                        unitTotal += cartOption.getPrice();
                        anyOptionInvalid = true;
                        continue;
                    }

                    CartDto.OptionStatus optStatus;
                    if (!current.getPrice().equals(cartOption.getPrice())) {
                        optStatus = CartDto.OptionStatus.PRICE_CHANGED;
                        anyPriceChanged = true;
                    } else if (!current.getName().equals(cartOption.getName())) {
                        optStatus = CartDto.OptionStatus.NAME_UPDATED;
                    } else {
                        optStatus = CartDto.OptionStatus.VALID;
                    }

                    reconciledOptions.add(new CartDto.Option(
                            displayGroupName, current.getName(), current.getPrice(), optStatus));
                    unitTotal += current.getPrice();
                }
            }

            // 5-D. 라인 상태 결정 (메뉴 OPEN 여부 → 옵션 invalid → 가격 변경 → VALID 순)
            boolean priceChanged = anyPriceChanged || unitTotal != line.getUnitPrice();
            CartDto.ItemStatus itemStatus;
            if (!MENU_STATUS_OPEN.equals(menu.getStatus())) {
                itemStatus = CartDto.ItemStatus.MENU_NOT_OPEN;
                messages.add("판매중이 아닌 메뉴입니다.");
            } else if (anyOptionInvalid) {
                itemStatus = CartDto.ItemStatus.INVALID_OPTION;
                messages.add("일부 옵션이 더 이상 제공되지 않습니다. 다시 선택해주세요.");
            } else if (priceChanged) {
                itemStatus = CartDto.ItemStatus.PRICE_CHANGED;
                messages.add("가격이 변경되었습니다.");
            } else {
                itemStatus = CartDto.ItemStatus.VALID;
            }

            resultItems.add(new CartDto.Item(
                    line.getId(),
                    line.getMenuId(),
                    menu.getName(),
                    unitTotal,
                    line.getMenuCount(),
                    reconciledOptions,
                    itemStatus,
                    List.copyOf(messages)
            ));

            totalPrice += unitTotal * line.getMenuCount();
        }

        // 6. 가게 정보 (있다면)
        CartDto.ShopBrief shop = cart.getShopId() == null ? null : loadShopBrief(cart.getShopId());

        return new CartDto.CartResponse(
                cart.getId(),
                sessionId,
                shop,
                resultItems,
                totalPrice
        );
    }

    private CartDto.ShopBrief loadShopBrief(Long shopId) {
        Shop shop = shopService.loadShopOrThrow(shopId);
        return new CartDto.ShopBrief(
                shop.getId(),
                shop.getName(),
                0L,
                shop.getMinOrderPrice(),
                shopService.isShopOpen(shop)
        );
    }

    // ====================================================================
    // 가격 조회
    // ====================================================================

    @Transactional(readOnly = true)
    public long getTotalPrice(Long userId) {
        Cart cart = cartRepository.findByUserId(userId).orElse(null);
        if (cart == null || cart.getItems().isEmpty()) {
            return 0L;
        }

        Map<Long, Menu> menus = loadMenus(cart);
        Map<Long, OptionGroup> ogs = loadOptionGroups(menus);

        long total = 0L;
        for (CartLineItem line : cart.getItems()) {
            total += calculateUnitPrice(line, menus, ogs) * line.getMenuCount();
        }
        return total;
    }

    private long calculateUnitPrice(CartLineItem line,
                                    Map<Long, Menu> menus,
                                    Map<Long, OptionGroup> ogs) {
        Menu menu = menus.get(line.getMenuId());
        if (menu == null) {
            return line.getUnitPrice();
        }

        long unit = menu.getBasePrice();
        for (CartOptionGroup cg : line.getGroups()) {
            unit += calculateOptionPrice(cg, ogs);
        }
        return unit;
    }

    private long calculateOptionPrice(CartOptionGroup cg, Map<Long, OptionGroup> ogs) {
        OptionGroup og = ogs.get(cg.getOptionGroupId());
        if (og == null) {
            return 0L;
        }
        Map<Long, Option> optById = new HashMap<>();
        for (Option o : og.getOptions()) {
            optById.put(o.getId(), o);
        }
        long sum = 0L;
        for (CartOption co : cg.getOptions()) {
            Option current = optById.get(co.getOptionId());
            if (current != null) {
                sum += current.getPrice();
            }
        }
        return sum;
    }

    private Map<Long, Menu> loadMenus(Cart cart) {
        Set<Long> menuIds = new HashSet<>();
        for (CartLineItem item : cart.getItems()) {
            menuIds.add(item.getMenuId());
        }
        Map<Long, Menu> result = new HashMap<>();
        for (Menu m : menuRepository.findAllById(menuIds)) {
            result.put(m.getId(), m);
        }
        return result;
    }

    private Map<Long, OptionGroup> loadOptionGroups(Map<Long, Menu> menus) {
        Set<Long> ogIds = new HashSet<>();
        for (Menu m : menus.values()) {
            for (MenuOptionGroup mog : m.getOptionGroups()) {
                ogIds.add(mog.getOptionGroupId());
            }
        }
        Map<Long, OptionGroup> result = new HashMap<>();
        if (!ogIds.isEmpty()) {
            for (OptionGroup og : optionGroupRepository.findAllById(ogIds)) {
                result.put(og.getId(), og);
            }
        }
        return result;
    }

    // ====================================================================
    // 아이템 추가
    // ====================================================================
    @Transactional
    public CartDto.CartResponse addItem(Long userId, AddItemRequest request) {
        validateAddItemRequest(userId, request);

        Cart cart = findOrCreateCart(userId);
        Menu menu = loadOpenMenu(request.menuId());
        switchShopIfNeeded(cart, menu.getShopId());
        validateSelectedOptions(menu, request.selectedOptions());

        CartLineItem line = buildCartLineItem(menu, request);
        addOrCombine(cart, line);
        cartRepository.save(cart);

        return getCart(userId, request.sessionId());
    }

    private void validateAddItemRequest(Long userId, AddItemRequest request) {
        if (userId == null) {
            throw new IllegalArgumentException("사용자 ID는 null이어서는 안됩니다.");
        }
        if (request == null) {
            throw new IllegalArgumentException("장바구니 아이템 요청은 null이어서는 안됩니다.");
        }
        if (request.menuId() == null) {
            throw new IllegalArgumentException("메뉴 ID는 필수입니다.");
        }
        if (request.quantity() == null || request.quantity() < 1) {
            throw new IllegalArgumentException("수량은 1 이상이어야 합니다: " + request.quantity());
        }
    }

    private Cart findOrCreateCart(Long userId) {
        Cart cart = cartRepository.findByUserId(userId).orElse(null);
        if (cart == null) {
            cart = new Cart();
            cart.setUserId(userId);
            cart = cartRepository.save(cart);
        }
        return cart;
    }

    private Menu loadOpenMenu(Long menuId) {
        Menu menu = menuRepository.findById(menuId)
                .orElseThrow(() -> new IllegalArgumentException("메뉴를 찾을 수 없습니다: " + menuId));
        if (menu.getShopId() == null) {
            throw new IllegalArgumentException("가게 ID는 null이어서는 안됩니다.");
        }
        if (!MENU_STATUS_OPEN.equals(menu.getStatus())) {
            throw new IllegalStateException("판매중이 아닌 메뉴입니다: " + menu.getName());
        }
        return menu;
    }

    private void switchShopIfNeeded(Cart cart, Long shopId) {
        if (cart.getShopId() != null && !cart.getShopId().equals(shopId)) {
            cart.getItems().clear();
        }
        cart.setShopId(shopId);
    }

    private void validateSelectedOptions(Menu menu, List<SelectedOption> selectedOptions) {
        Set<Long> validGroupIds = new HashSet<>();
        for (MenuOptionGroup mog : menu.getOptionGroups()) {
            validGroupIds.add(mog.getOptionGroupId());
        }
        Map<Long, List<SelectedOption>> grouped = groupSelectedOptions(selectedOptions);

        Map<Long, OptionGroup> ogById = new HashMap<>();
        if (!grouped.isEmpty()) {
            for (OptionGroup og : optionGroupRepository.findAllById(grouped.keySet())) {
                ogById.put(og.getId(), og);
            }
        }

        for (Map.Entry<Long, List<SelectedOption>> entry : grouped.entrySet()) {
            Long ogId = entry.getKey();
            if (!validGroupIds.contains(ogId)) {
                throw new IllegalArgumentException("해당 메뉴의 옵션 그룹이 아닙니다: " + ogId);
            }
            OptionGroup og = ogById.get(ogId);
            if (og == null) {
                throw new IllegalArgumentException("존재하지 않는 옵션 그룹입니다: " + ogId);
            }
            Set<Long> validOptionIds = new HashSet<>();
            for (Option mo : og.getOptions()) {
                validOptionIds.add(mo.getId());
            }
            for (SelectedOption sel : entry.getValue()) {
                if (!validOptionIds.contains(sel.optionId())) {
                    throw new IllegalArgumentException("해당 옵션 그룹의 옵션이 아닙니다: " + sel.optionId());
                }
            }
        }
    }

    private CartLineItem buildCartLineItem(Menu menu, AddItemRequest request) {
        Map<Long, List<SelectedOption>> grouped = groupSelectedOptions(request.selectedOptions());

        CartLineItem line = new CartLineItem();
        line.setMenuId(menu.getId());
        line.setMenuName(request.menuName() != null ? request.menuName() : menu.getName());
        line.setMenuCount(request.quantity());
        line.setUnitPrice(computeUnitPrice(menu, request.selectedOptions()));

        for (Map.Entry<Long, List<SelectedOption>> entry : grouped.entrySet()) {
            CartOptionGroup cog = new CartOptionGroup();
            cog.setOptionGroupId(entry.getKey());
            cog.setName(entry.getValue().get(0).optionGroupName());

            for (SelectedOption sel : entry.getValue()) {
                CartOption co = new CartOption();
                co.setOptionId(sel.optionId());
                co.setName(sel.name());
                co.setPrice(sel.price() == null ? 0L : sel.price());
                cog.getOptions().add(co);
            }
            line.getGroups().add(cog);
        }
        return line;
    }

    private void addOrCombine(Cart cart, CartLineItem line) {
        CartLineItem existing = findMatchingLine(cart, line);
        if (existing != null) {
            existing.setMenuCount(existing.getMenuCount() + line.getMenuCount());
        } else {
            cart.getItems().add(line);
        }
    }

    private CartLineItem findMatchingLine(Cart cart, CartLineItem newLine) {
        for (CartLineItem existing : cart.getItems()) {
            if (matchesContent(existing, newLine)) {
                return existing;
            }
        }
        return null;
    }

    private boolean matchesContent(CartLineItem a, CartLineItem b) {
        if (!a.getMenuId().equals(b.getMenuId())) {
            return false;
        }
        if (a.getGroups().size() != b.getGroups().size()) {
            return false;
        }
        for (int i = 0; i < a.getGroups().size(); i++) {
            if (!matchesOptionGroup(a.getGroups().get(i), b.getGroups().get(i))) {
                return false;
            }
        }
        return true;
    }

    private boolean matchesOptionGroup(CartOptionGroup a, CartOptionGroup b) {
        if (!a.getOptionGroupId().equals(b.getOptionGroupId())) {
            return false;
        }
        if (a.getOptions().size() != b.getOptions().size()) {
            return false;
        }
        Set<String> aKeys = new HashSet<>();
        for (CartOption o : a.getOptions()) {
            aKeys.add(o.getName() + ":" + o.getPrice());
        }
        for (CartOption o : b.getOptions()) {
            if (!aKeys.contains(o.getName() + ":" + o.getPrice())) {
                return false;
            }
        }
        return true;
    }

    private long computeUnitPrice(Menu menu, List<SelectedOption> selected) {
        long sum = menu.getBasePrice() == null ? 0L : menu.getBasePrice();
        if (selected != null) {
            for (SelectedOption sel : selected) {
                if (sel.price() != null) {
                    sum += sel.price();
                }
            }
        }
        return sum;
    }

    private Map<Long, List<SelectedOption>> groupSelectedOptions(List<SelectedOption> selected) {
        Map<Long, List<SelectedOption>> grouped = new LinkedHashMap<>();
        if (selected == null) {
            return grouped;
        }
        for (SelectedOption sel : selected) {
            grouped.computeIfAbsent(sel.optionGroupId(), k -> new ArrayList<>()).add(sel);
        }
        return grouped;
    }

    // ====================================================================
    // 수량 변경
    // ====================================================================
    @Transactional
    public CartDto.CartResponse updateQuantity(Long userId, Long itemId, Integer quantity, String sessionId) {
        if (quantity == null) {
            throw new IllegalArgumentException("수량은 필수입니다.");
        }
        if (quantity < 0) {
            throw new IllegalArgumentException("수량은 0 이상이어야 합니다.");
        }

        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("'" + userId + "'의 카트가 없습니다."));

        if (quantity == 0) {
            cart.getItems().removeIf(it -> it.getId().equals(itemId));
        } else {
            boolean found = false;
            for (CartLineItem it : cart.getItems()) {
                if (it.getId().equals(itemId)) {
                    it.setMenuCount(quantity);
                    found = true;
                    break;
                }
            }
            if (!found) {
                throw new IllegalArgumentException("카트 아이템이 없습니다: " + itemId);
            }
        }

        if (cart.getItems().isEmpty()) {
            cart.setShopId(null);
        }
        cartRepository.save(cart);

        return getCart(userId, sessionId);
    }

    // ====================================================================
    // 아이템 삭제
    // ====================================================================
    @Transactional
    public CartDto.CartResponse removeItem(Long userId, Long itemId, String sessionId) {
        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("'" + userId + "'의 카트가 없습니다."));

        boolean removed = cart.getItems().removeIf(it -> it.getId().equals(itemId));
        if (!removed) {
            throw new IllegalArgumentException("카트 아이템이 없습니다: " + itemId);
        }

        if (cart.getItems().isEmpty()) {
            cart.setShopId(null);
        }
        cartRepository.save(cart);

        return getCart(userId, sessionId);
    }

    // ====================================================================
    // 주문 전환
    // ====================================================================
    @Transactional
    public CartDto.OrderPlacedResponse placeOrder(Long userId) {
        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("'" + userId + "'의 카트가 없습니다."));

        // 0. 가게 + 카탈로그 한 번에 로드
        Shop shop = cart.getShopId() == null ? null : shopService.loadShopOrThrow(cart.getShopId());

        Set<Long> menuIds = new HashSet<>();
        for (CartLineItem item : cart.getItems()) {
            menuIds.add(item.getMenuId());
        }
        Map<Long, Menu> menusById = new HashMap<>();
        if (!menuIds.isEmpty()) {
            for (Menu m : menuRepository.findAllById(menuIds)) {
                menusById.put(m.getId(), m);
            }
        }

        Set<Long> ogIds = new HashSet<>();
        for (Menu m : menusById.values()) {
            for (MenuOptionGroup mog : m.getOptionGroups()) {
                ogIds.add(mog.getOptionGroupId());
            }
        }
        Map<Long, OptionGroup> ogById = new HashMap<>();
        if (!ogIds.isEmpty()) {
            for (OptionGroup og : optionGroupRepository.findAllById(ogIds)) {
                ogById.put(og.getId(), og);
            }
        }

        // 1. 주문 자격 검증
        validateOrderable(cart, shop, menusById, ogById);

        // 2. snapshot 생성 (검증 통과 이후 — 안전하게 lookup)
        long total = 0L;
        List<OrderLineItem> snapshots = new ArrayList<>();
        for (CartLineItem line : cart.getItems()) {
            Menu menu = menusById.get(line.getMenuId());
            long unit = menu.getBasePrice();
            List<OrderLineItem.OrderOptionGroup> snapGroups = new ArrayList<>();

            for (CartOptionGroup cg : line.getGroups()) {
                OptionGroup og = ogById.get(cg.getOptionGroupId());
                Map<Long, Option> optById = new HashMap<>();
                for (Option o : og.getOptions()) {
                    optById.put(o.getId(), o);
                }

                List<OrderLineItem.OrderOption> snapOpts = new ArrayList<>();
                for (CartOption co : cg.getOptions()) {
                    Option current = optById.get(co.getOptionId());
                    unit += current.getPrice();
                    snapOpts.add(new OrderLineItem.OrderOption(current.getName(), current.getPrice()));
                }
                snapGroups.add(new OrderLineItem.OrderOptionGroup(og.getName(), snapOpts));
            }

            OrderLineItem snap = new OrderLineItem();
            snap.setMenuId(menu.getId());
            snap.setMenuName(menu.getName());
            snap.setCount(line.getMenuCount());
            snap.setUnitPrice(unit);
            snap.setGroups(snapGroups);
            snapshots.add(snap);

            total += unit * line.getMenuCount();
        }

        // 3. Order 저장
        Order order = new Order();
        order.setUserId(userId);
        order.setShopId(shop.getId());
        order.setOrderedTime(LocalDateTime.now());
        order.setTotalPrice(total);
        order.setItemsSnapshot(snapshots);
        order = orderRepository.save(order);

        // 4. 카트 비우기
        cart.getItems().clear();
        cart.setShopId(null);
        cartRepository.save(cart);

        return new CartDto.OrderPlacedResponse(order.getId(), total);
    }

    /**
     * Cart → Order 변환 자격 검증. 위반 시 ISE.
     * <ol>
     *   <li>Cart가 비어있지 않음</li>
     *   <li>Cart shopId == 주문 Shop id 일치</li>
     *   <li>shop.isOpen()</li>
     *   <li>cart.totalPrice >= shop.minOrderPrice</li>
     *   <li>각 라인: 메뉴 OPEN + 옵션그룹/옵션 id 기반 매칭 + 가격 일치</li>
     * </ol>
     */
    void validateOrderable(Cart cart,
                           Shop shop,
                           Map<Long, Menu> menusById,
                           Map<Long, OptionGroup> ogById) {
        // 1. Cart 비어있지 않음
        if (cart.getItems().isEmpty()) {
            throw new IllegalStateException("장바구니가 비어 있어 주문할 수 없습니다.");
        }

        // 2. Shop id 일치
        if (shop == null || cart.getShopId() == null || !cart.getShopId().equals(shop.getId())) {
            throw new IllegalStateException("주문하려는 가게와 장바구니의 가게가 일치하지 않습니다.");
        }

        // 3. 가게 영업 중
        if (!shopService.isShopOpen(shop)) {
            throw new IllegalStateException("가게가 영업중이어야 합니다.");
        }

        // 5-pre. 라인별 strict 검증 + 가격 합계
        long total = 0L;
        for (CartLineItem line : cart.getItems()) {
            Menu menu = menusById.get(line.getMenuId());

            if (menu == null) {
                throw new IllegalStateException("메뉴를 찾을 수 없습니다: " + line.getMenuName());
            }

            if (!MENU_STATUS_OPEN.equals(menu.getStatus())) {
                throw new IllegalStateException("판매중이 아닌 메뉴입니다: " + menu.getName());
            }

            Set<Long> validGroupIds = new HashSet<>();
            for (MenuOptionGroup mog : menu.getOptionGroups()) {
                validGroupIds.add(mog.getOptionGroupId());
            }

            long unit = menu.getBasePrice();
            for (CartOptionGroup cg : line.getGroups()) {
                OptionGroup og = ogById.get(cg.getOptionGroupId());

                if (og == null || !validGroupIds.contains(og.getId())) {
                    throw new IllegalStateException("옵션 그룹이 더 이상 존재하지 않습니다: " + cg.getName());
                }

                Map<Long, Option> optById = new HashMap<>();
                for (Option o : og.getOptions()) {
                    optById.put(o.getId(), o);
                }

                for (CartOption co : cg.getOptions()) {
                    Option current = optById.get(co.getOptionId());

                    if (current == null) {
                        throw new IllegalStateException("옵션이 더 이상 존재하지 않습니다: " + co.getName());
                    }

                    if (!current.getPrice().equals(co.getPrice())) {
                        throw new IllegalStateException("옵션 가격이 변경되었습니다: " + co.getName());
                    }

                    unit += current.getPrice();
                }
            }

            // line snapshot unit_price와도 일치
            if (line.getUnitPrice() == null || line.getUnitPrice() != unit) {
                throw new IllegalStateException("가격이 변경되었습니다: " + line.getMenuName());
            }

            total += unit * line.getMenuCount();
        }

        // 4. 최소 주문 금액
        if (shop.getMinOrderPrice() != null && total < shop.getMinOrderPrice()) {
            throw new IllegalStateException("최소 주문금액 " + shop.getMinOrderPrice() + "원 이상이어야 합니다.");
        }
    }
}
