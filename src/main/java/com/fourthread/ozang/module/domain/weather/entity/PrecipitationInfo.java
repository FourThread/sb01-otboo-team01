package com.fourthread.ozang.module.domain.weather.entity;

import com.fourthread.ozang.module.domain.weather.dto.PrecipitationDto;
import com.fourthread.ozang.module.domain.weather.dto.type.PrecipitationType;
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
class PrecipitationInfo {
    @Enumerated(EnumType.STRING)
    private PrecipitationType type = PrecipitationType.NONE;
    private Double amount = 0.0;
    private Double probability = 0.0;

    public PrecipitationDto toDto() {
        return new PrecipitationDto(type, amount, probability);
    }
}