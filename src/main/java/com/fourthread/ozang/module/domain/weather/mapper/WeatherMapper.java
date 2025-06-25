package com.fourthread.ozang.module.domain.weather.mapper;

import com.fourthread.ozang.module.domain.weather.dto.WeatherAPILocation;
import com.fourthread.ozang.module.domain.weather.dto.WeatherApiResponse;
import com.fourthread.ozang.module.domain.weather.dto.WeatherApiResponse.WeatherItem;
import com.fourthread.ozang.module.domain.weather.dto.WeatherDto;
import com.fourthread.ozang.module.domain.weather.dto.WeatherSummaryDto;
import com.fourthread.ozang.module.domain.weather.entity.SkyStatus;
import com.fourthread.ozang.module.domain.weather.entity.Weather;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface WeatherMapper {

    default WeatherDto toDto(Weather entity) {
        if (entity == null) return null;

        return new WeatherDto(
            entity.getId(),
            entity.getForecastedAt(),
            entity.getForecastAt(),
            entity.getLocation(),
            entity.getSkyStatus(),
            entity.getPrecipitation(),
            entity.getHumidity(),
            entity.getTemperature(),
            entity.getWindSpeed()
        );
    }

    default WeatherSummaryDto toSummaryDto(Weather entity) {
        if (entity == null) return null;

        return new WeatherSummaryDto(
            entity.getId(),
            entity.getSkyStatus(),
            entity.getPrecipitation(),
            entity.getTemperature()
        );
    }

    default Weather fromApiResponse(List<WeatherItem> items, WeatherAPILocation location) {
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("Weather items cannot be null or empty");
        }
        if (location == null) {
            throw new IllegalArgumentException("Location cannot be null");
        }

        WeatherItem firstItem = items.get(0);
        if (firstItem == null) {
            throw new IllegalArgumentException("First weather item cannot be null");
        }

        // =============== 안전한 날짜/시간 파싱 ===============
        LocalDateTime forecastedAt = parseDateTime(firstItem.baseDate(), firstItem.baseTime());
        LocalDateTime forecastAt = parseDateTime(firstItem.fcstDate(), firstItem.fcstTime());

        // =============== Weather 엔티티 생성 ===============
        Weather entity = new Weather(forecastedAt, forecastAt, location, SkyStatus.CLEAR);

        // =============== 안전한 카테고리별 데이터 적용 ===============
        for (WeatherItem item : items) {
            if (item != null && item.category() != null && item.fcstValue() != null) {
                try {
                    entity.updateWeatherData(item.category(), item.fcstValue());
                } catch (Exception e) {
                    // 개별 항목 파싱 실패는 로그만 남기고 계속 진행
                    System.err.println("⚠️ Weather data update failed for category: " +
                        item.category() + ", value: " + item.fcstValue() + " - " + e.getMessage());
                }
            }
        }

        return entity;
    }

    default LocalDateTime parseDateTime(String date, String time) {
        try {
            if (date == null || time == null) {
                throw new IllegalArgumentException("Date or time is null");
            }

            // 시간이 4자리가 아닌 경우 앞에 0 패딩
            String paddedTime = time.length() < 4 ?
                String.format("%04d", Integer.parseInt(time)) : time;

            String datetime = date + paddedTime.substring(0, 2) + "00";
            return LocalDateTime.parse(datetime, DateTimeFormatter.ofPattern("yyyyMMddHHmm"));
        } catch (Exception e) {
            // 파싱 실패 시 현재 시간 반환
            System.err.println("⚠️ DateTime parsing failed for date: " + date + ", time: " + time + " - " + e.getMessage());
            return LocalDateTime.now();
        }
    }
}
//@Mapper(componentModel = "spring")
//public interface WeatherMapper {
//
//    default WeatherDto toDto(Weather entity) {
//        if (entity == null) return null;
//
//        return new WeatherDto(
//            entity.getId(),
//            entity.getForecastedAt(),
//            entity.getForecastAt(),
//            entity.getLocation(),
//            entity.getSkyStatus(),
//            entity.getPrecipitation(),
//            entity.getHumidity(),
//            entity.getTemperature(),
//            entity.getWindSpeed()
//        );
//    }
//
//    default WeatherSummaryDto toSummaryDto(Weather entity) {
//        if (entity == null) return null;
//
//        return new WeatherSummaryDto(
//            entity.getId(),
//            entity.getSkyStatus(),
//            entity.getPrecipitation(),
//            entity.getTemperature()
//        );
//    }
//
//    default Weather fromApiResponse(List<WeatherItem> items, WeatherAPILocation location) {
//        if (items == null || items.isEmpty()) return null;
//
//        WeatherApiResponse.WeatherItem firstItem = items.get(0);
//
//        LocalDateTime forecastedAt = parseDateTime(firstItem.baseDate(), firstItem.baseTime());
//        LocalDateTime forecastAt = parseDateTime(firstItem.fcstDate(), firstItem.fcstTime());
//
//        Weather entity = new Weather(forecastedAt, forecastAt, location, SkyStatus.CLEAR);
//
//        // 카테고리별 데이터 적용
//        for (WeatherApiResponse.WeatherItem item : items) {
//            entity.updateWeatherData(item.category(), item.fcstValue());
//        }
//
//        return entity;
//    }
//
//    default LocalDateTime parseDateTime(String date, String time) {
//        String datetime = date + time.substring(0, 2) + "00";
//        return LocalDateTime.parse(datetime, DateTimeFormatter.ofPattern("yyyyMMddHHmm"));
//    }
//}