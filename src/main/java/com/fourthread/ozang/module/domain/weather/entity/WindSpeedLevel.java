package com.fourthread.ozang.module.domain.weather.entity;

public enum WindSpeedLevel {
    WEAK("약함"),
    MODERATE("보통"),
    STRONG("강함");

    private final String description;

    WindSpeedLevel(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public static WindSpeedLevel fromSpeed(Double speed) {
        if (speed == null) {
            return WEAK;
        }
        if (speed < 4.0) {
            return WEAK;
        }
        if (speed < 9.0) {
            return MODERATE;
        }
        return STRONG;
    }
}