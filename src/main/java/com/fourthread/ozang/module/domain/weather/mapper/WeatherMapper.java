package com.fourthread.ozang.module.domain.weather.mapper;

import com.fourthread.ozang.module.domain.weather.dto.HumidityDto;
import com.fourthread.ozang.module.domain.weather.dto.PrecipitationDto;
import com.fourthread.ozang.module.domain.weather.dto.TemperatureDto;
import com.fourthread.ozang.module.domain.weather.dto.WeatherAPILocation;
import com.fourthread.ozang.module.domain.weather.dto.WeatherDto;
import com.fourthread.ozang.module.domain.weather.dto.WeatherSummaryDto;
import com.fourthread.ozang.module.domain.weather.dto.WindSpeedDto;
import com.fourthread.ozang.module.domain.weather.entity.Weather;
import java.util.Arrays;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface WeatherMapper {

    @Mapping(target = "location", expression = "java(mapToWeatherAPILocation(weather))")
    @Mapping(target = "precipitation", expression = "java(mapToPrecipitationDto(weather))")
    @Mapping(target = "humidity", expression = "java(mapToHumidityDto(weather))")
    @Mapping(target = "temperature", expression = "java(mapToTemperatureDto(weather))")
    @Mapping(target = "windSpeed", expression = "java(mapToWindSpeedDto(weather))")
    WeatherDto toDto(Weather weather);

    @Mapping(target = "precipitation", expression = "java(mapToPrecipitationDto(weather))")
    @Mapping(target = "temperature", expression = "java(mapToTemperatureDto(weather))")
    WeatherSummaryDto toSummaryDto(Weather weather);

    default WeatherAPILocation mapToWeatherAPILocation(Weather weather) {
        List<String> locationNames = weather.getLocationNames() != null ?
            Arrays.asList(weather.getLocationNames().split(",")) :
            List.of();

        return new WeatherAPILocation(
            weather.getLatitude(),
            weather.getLongitude(),
            weather.getX(),
            weather.getY(),
            locationNames
        );
    }

    default PrecipitationDto mapToPrecipitationDto(Weather weather) {
        return new PrecipitationDto(
            weather.getPrecipitationType(),
            weather.getPrecipitationAmount(),
            weather.getPrecipitationProbability()
        );
    }

    default HumidityDto mapToHumidityDto(Weather weather) {
        return new HumidityDto(
            weather.getCurrentHumidity(),
            weather.getComparedToDayBeforeHumidity()
        );
    }

    default TemperatureDto mapToTemperatureDto(Weather weather) {
        return new TemperatureDto(
            weather.getCurrentTemperature(),
            weather.getComparedToDayBeforeTemperature(),
            weather.getMinTemperature(),
            weather.getMaxTemperature()
        );
    }

    default WindSpeedDto mapToWindSpeedDto(Weather weather) {
        return new WindSpeedDto(
            weather.getWindSpeed(),
            weather.getWindStrength()
        );
    }

    default WeatherAPILocation toWeatherAPILocation(Double latitude, Double longitude,
        Integer x, Integer y, List<String> locationNames) {
        return new WeatherAPILocation(latitude, longitude, x, y, locationNames);
    }
}
