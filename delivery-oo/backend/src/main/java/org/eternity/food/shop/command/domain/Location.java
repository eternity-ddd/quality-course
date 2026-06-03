package org.eternity.food.shop.command.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.eternity.food.base.domain.ValueObject;

@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Location extends ValueObject<Location> {

    @Column(name = "LATITUDE")
    private Double latitude;

    @Column(name = "LONGITUDE")
    private Double longitude;

    public Location(Double latitude, Double longitude) {
        if (latitude == null || longitude == null) {
            throw new IllegalArgumentException("좌표는 null이어서는 안됩니다.");
        }

        if (latitude < -90 || latitude > 90) {
            throw new IllegalArgumentException("위도는 -90~90 범위여야 합니다: " + latitude);
        }

        if (longitude < -180 || longitude > 180) {
            throw new IllegalArgumentException("경도는 -180~180 범위여야 합니다: " + longitude);
        }

        this.latitude = latitude;
        this.longitude = longitude;
    }
}
