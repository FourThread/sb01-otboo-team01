package com.fourthread.ozang.module.domain.weather.dto.type;

public enum WindStrength {
    WEAK("약함"),
    MODERATE("보통"),
    STRONG("강함");

    private final String description;

    WindStrength(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public static WindStrength fromSpeed(double speed) {
        if (speed < 4.0) {
            return WEAK;
        } else if (speed < 9.0) {
            return MODERATE;
        } else {
            return STRONG;
        }
    }
}