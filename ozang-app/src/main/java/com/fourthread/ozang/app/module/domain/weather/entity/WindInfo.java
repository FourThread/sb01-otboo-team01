package com.fourthread.ozang.module.domain.weather.entity;

import com.fourthread.ozang.module.domain.weather.dto.WindSpeedDto;
import com.fourthread.ozang.module.domain.weather.dto.type.WindStrength;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
class WindInfo {
    private Double speed = 0.0;
    private Double direction = 0.0;
    private Double uComponent = 0.0;  // 동서 성분
    private Double vComponent = 0.0;  // 남북 성분

    @Enumerated(EnumType.STRING)
    private WindStrength strength = WindStrength.WEAK;

    public WindSpeedDto toDto() {
        return new WindSpeedDto(speed, strength);
    }
}