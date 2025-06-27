package com.fourthread.ozang.module.domain.weather.dto.type;

public enum SkyStatus {
    CLEAR("맑음"),
    MOSTLY_CLOUDY("구름많음"),
    CLOUDY("흐림");

    private final String description;

    SkyStatus(String description) {
        this.description = description;
    }

    public static SkyStatus fromCode(String code) {
        return switch (code) {
            case "3" -> MOSTLY_CLOUDY;
            case "4" -> CLOUDY;
            default -> CLEAR;
        };
    }

    public String getDescription() {
        return description;
    }
}