package com.fourthread.ozang.module.domain.weather.service;

import com.fourthread.ozang.module.domain.weather.entity.WeatherAlert;

public record WeatherChangeEvent(WeatherAlert alert) {
}
