package com.fourthread.ozang.module.domain.weather.entity;

import com.fourthread.ozang.module.domain.weather.dto.TemperatureDto;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Embeddable
@Setter
@NoArgsConstructor
@AllArgsConstructor
class TemperatureInfo {
    @Column(name = "temperature_current")
    private Double current = 0.0;

    @Column(name = "temperature_compared_to_day_before")
    private Double comparedToDayBefore = 0.0;
    private Double min = 0.0;
    private Double max = 0.0;

    public TemperatureDto toDto() {
        return new TemperatureDto(current, comparedToDayBefore, min, max);
    }
}