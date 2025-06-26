package com.fourthread.ozang.module.domain.weather.dto.type;

public enum PrecipitationType {
    NONE("없음"),
    RAIN("비"),
    RAIN_SNOW("비/눈"),
    SNOW("눈"),
    SHOWER("소나기");

    private final String description;

    PrecipitationType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}