package com.fourthread.ozang.module.domain.weather.entity;

public enum PrecipitationType {
    NONE("없음", "0"),
    RAIN("비", "1"),
    RAIN_SNOW("비/눈", "2"),
    SNOW("눈", "3"),
    SHOWER("소나기", "4");

    private final String description;
    private final String code;

    PrecipitationType(String description, String code) {
        this.description = description;
        this.code = code;
    }

    public String getDescription() { return description; }
    public String getCode() { return code; }

    public static PrecipitationType fromCode(String code) {
        for (PrecipitationType type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        return NONE;
    }
}