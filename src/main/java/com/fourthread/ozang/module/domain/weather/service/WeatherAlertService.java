package com.fourthread.ozang.module.domain.weather.service;

import com.fourthread.ozang.module.domain.weather.entity.GridCoordinate;
import com.fourthread.ozang.module.domain.weather.entity.Weather;
import com.fourthread.ozang.module.domain.weather.entity.WeatherAlert;
import com.fourthread.ozang.module.domain.weather.entity.WeatherAlertType;
import com.fourthread.ozang.module.domain.weather.repository.WeatherAlertRepository;
import com.fourthread.ozang.module.domain.weather.repository.WeatherDataRepository;
import java.time.LocalDateTime;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class WeatherAlertService {

    private final WeatherDataRepository weatherDataRepository;
    private final WeatherAlertRepository weatherAlertRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public void processWeatherChanges(Weather currentWeather) {
        // 이전 날씨 데이터 조회
        Optional<Weather> previousWeatherOpt = findPreviousWeatherData(currentWeather);

        if (previousWeatherOpt.isEmpty()) {
            log.debug("No previous weather data found for comparison");
            return;
        }

        Weather previousWeather = previousWeatherOpt.get();
        GridCoordinate coordinate = new GridCoordinate(currentWeather.getX(), currentWeather.getY());

        // 기온 변화 감지
        checkTemperatureChanges(currentWeather, previousWeather, coordinate);

        // 강수 변화 감지
        checkPrecipitationChanges(currentWeather, previousWeather, coordinate);
    }

    private void checkTemperatureChanges(Weather current, Weather previous, GridCoordinate coordinate) {
        Double currentTemp = current.getTemperatureCurrent();
        Double previousTemp = previous.getTemperatureCurrent();

        if (currentTemp != null && previousTemp != null) {
            double tempDiff = currentTemp - previousTemp;

            // 5도 이상 온도 변화 시 알림
            if (Math.abs(tempDiff) >= 5.0) {
                WeatherAlertType alertType = tempDiff > 0 ?
                    WeatherAlertType.TEMPERATURE_RISE : WeatherAlertType.TEMPERATURE_DROP;

                WeatherAlert alert = WeatherAlert.createTemperatureAlert(coordinate, alertType, tempDiff);
                weatherAlertRepository.save(alert);

                // 알림 이벤트 발행
                eventPublisher.publishEvent(new WeatherChangeEvent(alert));

                log.info("Temperature change alert created: {} degrees difference at coordinate ({}, {})",
                    tempDiff, coordinate.getX(), coordinate.getY());
            }
        }
    }

    private void checkPrecipitationChanges(Weather current, Weather previous, GridCoordinate coordinate) {
        // 강수 타입 변화 감지
        if (current.getPrecipitationType() != previous.getPrecipitationType()) {
            WeatherAlertType alertType = current.getPrecipitationType().getCode().equals("0") ?
                WeatherAlertType.PRECIPITATION_CHANGE : WeatherAlertType.PRECIPITATION_START;

            WeatherAlert alert = WeatherAlert.createPrecipitationAlert(coordinate, alertType);
            weatherAlertRepository.save(alert);

            // 알림 이벤트 발행
            eventPublisher.publishEvent(new WeatherChangeEvent(alert));

            log.info("Precipitation change alert created: {} to {} at coordinate ({}, {})",
                previous.getPrecipitationType(), current.getPrecipitationType(),
                coordinate.getX(), coordinate.getY());
        }
    }

    private Optional<Weather> findPreviousWeatherData(Weather currentWeather) {
        LocalDateTime oneDayAgo = currentWeather.getForecastAt().minusDays(1);
        return weatherDataRepository.findByCoordinateAndForecastAt(
            currentWeather.getX(), currentWeather.getY(), oneDayAgo);
    }
}