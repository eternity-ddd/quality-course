package org.eternity.food.service;

import org.eternity.food.dto.MenuDto;
import org.eternity.food.entity.Menu;
import org.eternity.food.entity.MenuOptionGroup;
import org.eternity.food.entity.Option;
import org.eternity.food.entity.OptionGroup;
import org.eternity.food.repository.OptionRepository;
import org.eternity.food.repository.MenuRepository;
import org.eternity.food.repository.OptionGroupRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * 메뉴 조회/매핑/생성/상태 fat-service. 메뉴 → 옵션그룹 → 옵션 펼쳐서 DTO로 손수 빚는다.
 */
@Service
public class MenuService {

    /** 필수 옵션그룹 최대 개수. */
    public static final int MAX_REQUIRED_GROUP = 3;

    /** 필수 옵션그룹의 최소 옵션 개수. */
    public static final int MIN_REQUIRED_OPTION = 2;

    private static final String MENU_STATUS_OPEN = "OPEN";
    private static final String MENU_STATUS_READY = "READY";

    private final MenuRepository menuRepository;
    private final OptionGroupRepository optionGroupRepository;
    private final OptionRepository optionRepository;

    public MenuService(MenuRepository menuRepository,
                       OptionGroupRepository optionGroupRepository,
                       OptionRepository optionRepository) {
        this.menuRepository = menuRepository;
        this.optionGroupRepository = optionGroupRepository;
        this.optionRepository = optionRepository;
    }

    // ====================================================================
    // 조회
    // ====================================================================

    @Transactional(readOnly = true)
    public List<MenuDto.Item> findByShopId(Long shopId) {
        List<Menu> menus = menuRepository.findByShopIdOrderById(shopId);
        List<MenuDto.Item> result = new ArrayList<>();
        for (Menu menu : menus) {
            result.add(new MenuDto.Item(
                    menu.getId(),
                    menu.getName(),
                    menu.getDescription(),
                    menu.getBasePrice(),
                    null
            ));
        }
        return result;
    }

    @Transactional(readOnly = true)
    public MenuDto.Detail findDetail(Long menuId) {
        Menu menu = menuRepository.findById(menuId)
                .orElseThrow(() -> new IllegalArgumentException("메뉴를 찾을 수 없습니다: " + menuId));

        // displayOrder 순서대로 option group id 수집
        List<MenuOptionGroup> mogs = menu.getOptionGroups();

        // 한 번에 다 가져옴
        List<Long> ogIds = new ArrayList<>();
        for (MenuOptionGroup mog : mogs) {
            ogIds.add(mog.getOptionGroupId());
        }

        Map<Long, OptionGroup> ogById = new HashMap<>();
        if (!ogIds.isEmpty()) {
            List<OptionGroup> ogs = optionGroupRepository.findAllById(ogIds);
            for (OptionGroup og : ogs) {
                ogById.put(og.getId(), og);
            }
        }

        // displayOrder 순서대로 DTO 만들기
        List<MenuDto.Detail.OptionGroup> ogDtos = new ArrayList<>();
        for (MenuOptionGroup mog : mogs) {
            OptionGroup og = ogById.get(mog.getOptionGroupId());
            if (og == null) {
                continue; // 데이터 불일치는 일부러 silent skip (전형적인 절차지향)
            }
            List<MenuDto.Detail.Option> optDtos = new ArrayList<>();
            for (Option opt : og.getOptions()) {
                optDtos.add(new MenuDto.Detail.Option(opt.getId(), opt.getName(), opt.getPrice()));
            }
            ogDtos.add(new MenuDto.Detail.OptionGroup(
                    og.getId(), og.getName(), Boolean.TRUE.equals(og.getRequired()), optDtos));
        }

        return new MenuDto.Detail(
                menu.getId(),
                menu.getName(),
                menu.getDescription(),
                menu.getBasePrice(),
                null,
                ogDtos
        );
    }

    // ====================================================================
    // 생성 (Menu / OptionGroup / Option)
    // ====================================================================

    /**
     * Menu 생성. 구조 검증 + MenuConfiguration + 각 MenuOptionGroup 검증 후 저장.
     * 상태는 READY로 시작 (판매 자격 검증 없이 등록 가능).
     */
    @Transactional
    public Menu create(Long shopId,
                       String name,
                       String description,
                       Long basePrice,
                       List<MenuOptionGroup> optionGroups) {
        validateMenuStructure(basePrice, optionGroups);
        validateMenuConfiguration(optionGroups);
        for (MenuOptionGroup mog : optionGroups) {
            validateMenuOptionGroup(mog);
        }

        Menu menu = new Menu();
        menu.setShopId(shopId);
        menu.setName(name);
        menu.setDescription(description);
        menu.setBasePrice(basePrice);
        menu.setStatus(MENU_STATUS_READY);
        menu.setOptionGroups(new ArrayList<>(optionGroups));

        return menuRepository.save(menu);
    }

    /**
     * OptionGroup 생성. OptionGroup + 옵션들의 Option 검증 후 저장.
     */
    @Transactional
    public OptionGroup createOptionGroup(String name, boolean required, List<Option> options) {
        validateOptionGroup(name, options, required);
        for (Option opt : options) {
            validateOption(opt == null ? null : opt.getName(), opt == null ? null : opt.getPrice());
        }

        OptionGroup og = new OptionGroup();
        og.setName(name);
        og.setRequired(required);
        og.setOptions(new ArrayList<>(options));

        return optionGroupRepository.save(og);
    }

    /**
     * Option 단독 생성 (OptionGroup에 종속). Option 검증 후 저장.
     */
    @Transactional
    public Option createOption(Long optionGroupId, String name, Long price) {
        validateOption(name, price);
        if (optionGroupId == null) {
            throw new IllegalArgumentException("optionGroupId는 null이어서는 안됩니다.");
        }

        Option opt = new Option();
        opt.setOptionGroupId(optionGroupId);
        opt.setName(name);
        opt.setPrice(price);

        return optionRepository.save(opt);
    }

    // ====================================================================
    // 상태 머신 (READY <-> OPEN)
    // ====================================================================

    /**
     * 메뉴 판매 시작. READY → OPEN. 판매 자격 충족 시에만 허용.
     * 이미 OPEN이면 멱등 (no-op).
     */
    @Transactional
    public void openMenu(Long menuId) {
        Menu menu = menuRepository.findById(menuId)
                .orElseThrow(() -> new IllegalArgumentException("메뉴를 찾을 수 없습니다: " + menuId));

        if (MENU_STATUS_OPEN.equals(menu.getStatus())) {
            return;
        }

        List<Long> ogIds = new ArrayList<>();
        for (MenuOptionGroup mog : menu.getOptionGroups()) {
            ogIds.add(mog.getOptionGroupId());
        }
        List<OptionGroup> optionGroups = ogIds.isEmpty()
                ? List.of()
                : optionGroupRepository.findAllById(ogIds);

        validateSellable(menu, optionGroups);

        menu.setStatus(MENU_STATUS_OPEN);
        menuRepository.save(menu);
    }

    /**
     * 메뉴 판매 중지. OPEN → READY 무조건. 이미 READY면 멱등 (no-op).
     */
    @Transactional
    public void closeMenu(Long menuId) {
        Menu menu = menuRepository.findById(menuId)
                .orElseThrow(() -> new IllegalArgumentException("메뉴를 찾을 수 없습니다: " + menuId));

        if (!MENU_STATUS_OPEN.equals(menu.getStatus())) {
            return;
        }

        menu.setStatus(MENU_STATUS_READY);
        menuRepository.save(menu);
    }

    // ====================================================================
    // 검증 메서드
    // ====================================================================

    /**
     * Menu 구조 검증:
     * <ul>
     *   <li>basePrice != null</li>
     *   <li>basePrice &gt; 0</li>
     *   <li>configuration != null — proc에서는 optionGroups 리스트 자체</li>
     * </ul>
     */
    void validateMenuStructure(Long basePrice, List<MenuOptionGroup> optionGroups) {
        if (basePrice == null) {
            throw new IllegalArgumentException("basePrice는 null이어서는 안됩니다.");
        }

        if (basePrice <= 0L) {
            throw new IllegalArgumentException("기본가는 0원보다는 커야합니다.");
        }

        if (optionGroups == null) {
            throw new IllegalArgumentException("configuration은 null이어서는 안됩니다.");
        }
    }

    /**
     * OptionGroup 구조 검증:
     * <ul>
     *   <li>name != null</li>
     *   <li>name.length >= 2</li>
     *   <li>options != null</li>
     *   <li>options.size >= 1</li>
     *   <li>그룹 내 이름 unique</li>
     *   <li>required=true && options &lt; MIN_REQUIRED_OPTION(2)이면 IAE</li>
     * </ul>
     */
    void validateOptionGroup(String name, List<Option> options, boolean required) {
        if (name == null || name.length() < 2) {
            throw new IllegalArgumentException("옵션그룹명은 2글자 이상이어야 합니다.");
        }

        if (options == null || options.isEmpty()) {
            throw new IllegalArgumentException("옵션은 1개 이상이어야 합니다.");
        }

        long uniqueNameCount = options.stream()
                .map(Option::getName)
                .distinct()
                .count();

        if (uniqueNameCount != options.size()) {
            throw new IllegalArgumentException("옵션 이름이 중복되어 있습니다.");
        }

        if (required && options.size() < MIN_REQUIRED_OPTION) {
            throw new IllegalArgumentException(
                    String.format("필수 옵션그룹의 옵션 갯수는 %d개 이상이어야 합니다.", MIN_REQUIRED_OPTION));
        }
    }

    /**
     * Option 구조 검증:
     * <ul>
     *   <li>name != null</li>
     *   <li>name.length >= 2</li>
     *   <li>price != null</li>
     *   <li>price >= 0</li>
     * </ul>
     */
    void validateOption(String name, Long price) {
        if (name == null || name.length() < 2) {
            throw new IllegalArgumentException("옵션명은 2글자 이상이어야 합니다.");
        }

        if (price == null) {
            throw new IllegalArgumentException("옵션 가격은 null이어서는 안됩니다.");
        }

        if (price < 0L) {
            throw new IllegalArgumentException("옵션 가격은 0원 이상이어야 합니다.");
        }
    }

    /**
     * MenuOptionGroup VO 구조 검증:
     * <ul>
     *   <li>optionGroupId != null</li>
     *   <li>displayOrder >= 1</li>
     * </ul>
     */
    void validateMenuOptionGroup(MenuOptionGroup mog) {
        if (mog == null) {
            throw new IllegalArgumentException("menuOptionGroup은 null이어서는 안됩니다.");
        }

        if (mog.getOptionGroupId() == null) {
            throw new IllegalArgumentException("optionGroupId는 null이어서는 안됩니다.");
        }

        if (mog.getDisplayOrder() == null || mog.getDisplayOrder() <= 0) {
            throw new IllegalArgumentException("displayOrder는 1 이상이어야 합니다.");
        }
    }

    /**
     * MenuConfiguration VO 구조 검증:
     * <ul>
     *   <li>menuOptionGroups != null</li>
     *   <li>같은 optionGroupId 중복 금지</li>
     * </ul>
     */
    void validateMenuConfiguration(List<MenuOptionGroup> menuOptionGroups) {
        if (menuOptionGroups == null) {
            throw new IllegalArgumentException("옵션그룹은 null이어서는 안됩니다.");
        }

        long distinctCount = menuOptionGroups.stream()
                .map(MenuOptionGroup::getOptionGroupId)
                .filter(Objects::nonNull)
                .distinct()
                .count();

        if (menuOptionGroups.size() != distinctCount) {
            throw new IllegalArgumentException("중복된 옵션 그룹을 포함합니다.");
        }
    }

    /**
     * 판매 가능 메뉴 자격 검증 — open / changeConfiguration 시점에 강제.
     * 위반 시 {@link IllegalStateException}.
     * <ul>
     *   <li>옵션그룹이 1개 이상 포함</li>
     *   <li>메뉴의 옵션그룹 ID = 실제 OptionGroup ID 일치</li>
     *   <li>필수 옵션그룹 ≤ {@link #MAX_REQUIRED_GROUP} (3)</li>
     * </ul>
     */
    void validateSellable(Menu menu, Collection<OptionGroup> optionGroups) {
        List<MenuOptionGroup> mogs = menu.getOptionGroups();

        if (mogs == null || mogs.isEmpty()) {
            throw new IllegalStateException("메뉴에는 옵션그룹이 1개 이상 포함되어야 합니다.");
        }

        Set<Long> configOptionGroupIds = new HashSet<>();
        for (MenuOptionGroup mog : mogs) {
            configOptionGroupIds.add(mog.getOptionGroupId());
        }

        Set<Long> optionGroupIds = new HashSet<>();
        for (OptionGroup og : optionGroups) {
            optionGroupIds.add(og.getId());
        }

        if (!configOptionGroupIds.equals(optionGroupIds)) {
            throw new IllegalStateException("옵션 그룹 구성이 일치하지 않습니다.");
        }

        long requiredCount = optionGroups.stream()
                .filter(og -> Boolean.TRUE.equals(og.getRequired()))
                .count();

        if (requiredCount > MAX_REQUIRED_GROUP) {
            throw new IllegalStateException(
                    String.format("필수 옵션그룹의 갯수는 %d개 이하여야 합니다.", MAX_REQUIRED_GROUP));
        }
    }
}
