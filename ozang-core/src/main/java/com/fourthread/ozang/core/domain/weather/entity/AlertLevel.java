package com.fourthread.ozang.core.domain.weather.entity;

public enum AlertLevel {
    INFO("정보"),
    WARNING("주의"),
    ERROR("경고");

    private final String description;

    AlertLevel(String description) {
        this.description = description;
    }

    public String getDescription() { return description; }
}
