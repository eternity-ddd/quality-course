package org.eternity.food.shop.command.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.eternity.food.base.domain.AggregateRoot;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;

@Entity
@Table(name = "OPTION_GROUP")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OptionGroup extends AggregateRoot<OptionGroup, Long> {
    public static final int MIN_REQUIRED_OPTION = 2;

    @Getter
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Getter
    private String name;

    @Getter
    private boolean required;

    @OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "OPTION_GROUP_ID")
    private Set<Option> options = new HashSet<>();

    public OptionGroup(String name, boolean required, Option option) {
        this(null, name, required, new HashSet<>(Set.of(option)));
    }

    public OptionGroup(String name, boolean required, Set<Option> options) {
        this(null, name, required, options);
    }

    @Builder
    public OptionGroup(Long id, String name, boolean required, Set<Option> options) {
        validateName(name);
        validateOptions(required, options);

        this.id = id;
        this.name = name;
        this.required = required;
        this.options = new HashSet<>(options);
    }

    void updateOptions(List<OptionPatch> patches) {
        Set<Long> incomingIds = patches.stream()
                .map(OptionPatch::id)
                .filter(Objects::nonNull)
                .collect(toSet());

        Map<Long, Option> existingById = options.stream()
                .collect(toMap(Option::getId, identity()));

        Set<Long> existingIds = existingById.keySet();
        Set<Long> unknownIds = incomingIds.stream()
                .filter(id -> !existingIds.contains(id))
                .collect(toSet());
        if (!unknownIds.isEmpty()) {
            throw new IllegalArgumentException("존재하지 않는 옵션 id: " + unknownIds);
        }

        options.removeIf(option -> !incomingIds.contains(option.getId()));

        for (OptionPatch patch : patches) {
            if (patch.id() == null) {
                options.add(new Option(patch.name(), patch.price()));
            } else {
                Option existing = existingById.get(patch.id());
                existing.rename(patch.name());
                existing.changePrice(patch.price());
            }
        }

        validateOptions(required, options);
    }

    private void validateName(String name) {
        if (name == null || name.length() < 2) {
            throw new IllegalArgumentException("옵션그룹명은 2글자 이상이어야 합니다.");
        }
    }

    private void validateOptions(boolean required, Set<Option> options) {
        if (options == null || options.isEmpty()) {
            throw new IllegalArgumentException("옵션은 1개 이상이어야 합니다.");
        }

        if (hasDuplicateOptionName(options)) {
            throw new IllegalArgumentException("옵션 이름이 중복되어 있습니다.");
        }

        if (required && options.size() < MIN_REQUIRED_OPTION) {
            throw new IllegalArgumentException(
                    String.format("필수 옵션그룹의 옵션 갯수는 %d개 이상이어야 합니다.", MIN_REQUIRED_OPTION));
        }
    }

    private boolean hasDuplicateOptionName(Set<Option> options) {
        long uniqueNameCount = options.stream()
                .map(Option::getName)
                .distinct()
                .count();

        return uniqueNameCount != options.size();
    }

    public int getOptionSize() {
        return options.size();
    }

    public Set<Option> getOptions() {
        return Collections.unmodifiableSet(options);
    }

    public Optional<Option> findOption(String name) {
        return options.stream()
                .filter(option -> option.getName().equals(name))
                .findFirst();
    }

    public Optional<Option> findOption(Long id) {
        return options.stream()
                .filter(option -> java.util.Objects.equals(option.getId(), id))
                .findFirst();
    }
}
