package com.fourthread.ozang.module.domain.weather.entity;

public enum SkyStatus {
    CLEAR("맑음", "1"),
    MOSTLY_CLOUDY("구름많음", "3"),
    CLOUDY("흐림", "4");

    private final String description;
    private final String code;

    SkyStatus(String description, String code) {
        this.description = description;
        this.code = code;
    }

    public String getDescription() { return description; }
    public String getCode() { return code; }

    public static SkyStatus fromCode(String code) {
        for (SkyStatus status : values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        return CLEAR;
    }
}