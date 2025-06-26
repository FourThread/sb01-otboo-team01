package com.fourthread.ozang.module.domain.weather.dto.type;

public enum SkyStatus {
    CLEAR("맑음"),
    MOSTLY_CLOUDY("구름많음"),
    CLOUDY("흐림");

    private final String description;

    SkyStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}