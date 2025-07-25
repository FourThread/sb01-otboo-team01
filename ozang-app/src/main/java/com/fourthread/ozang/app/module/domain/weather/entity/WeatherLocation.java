package com.fourthread.ozang.module.domain.weather.entity;

import com.fourthread.ozang.module.domain.weather.dto.WeatherAPILocation;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
class WeatherLocation {
    private Double latitude;
    private Double longitude;
    private Integer x;
    private Integer y;

    @Column(length = 500)
    private String locationNames;

    public static WeatherLocation from(WeatherAPILocation apiLocation) {
        return new WeatherLocation(
            apiLocation.latitude(),
            apiLocation.longitude(),
            apiLocation.x(),
            apiLocation.y(),
            String.join(",", apiLocation.locationNames())
        );
    }

    public WeatherAPILocation toApiLocation() {
        return new WeatherAPILocation(
            latitude, longitude, x, y,
            locationNames != null ? List.of(locationNames.split(",")) : List.of()
        );
    }
}