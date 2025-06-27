package com.fourthread.ozang.module.domain.weather.entity;

import com.fourthread.ozang.module.domain.weather.dto.HumidityDto;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
class HumidityInfo {
    @Column(name = "humidity_current")
    private Double current = 50.0;

    @Column(name = "humidity_compared_to_day_before")
    private Double comparedToDayBefore = 0.0;

    public HumidityDto toDto() {
        return new HumidityDto(current, comparedToDayBefore);
    }
}