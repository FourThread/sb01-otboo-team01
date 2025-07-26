package com.fourthread.ozang.app.domain.recommend.dto;

import com.fourthread.ozang.core.domain.clothes.entity.Clothes;
import com.fourthread.ozang.core.domain.clothes.entity.ClothesAttribute;
import com.fourthread.ozang.core.domain.weather.dto.type.PrecipitationType;
import com.fourthread.ozang.core.domain.weather.dto.type.SkyStatus;
import com.fourthread.ozang.core.domain.weather.entity.Weather;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Builder;

@Builder
public record RecommendRequest(

    LocalDateTime forecastedAt,
    LocalDateTime forecastAt,
    SkyStatus skyStatus,
    PrecipitationType precipitationType,
    Double precipitationAmount,
    Double precipitationProbability,
    Double currentTemperature,
    Double comparedToDayBeforeTemperature,
    Double minTemperature,
    Double maxTemperature,
    Double currentHumidity,
    Double comparedToDayBeforeHumidity,
    Double windSpeed,

    List<RecommendClothesDto> clothesData

) {

  public static RecommendRequest of(Weather weather, List<Clothes> clothes) {
    return new RecommendRequest(
        weather.getForecastedAt(),
        weather.getForecastAt(),
        weather.getSkyStatus(),
        weather.getPrecipitation().type(),
        weather.getPrecipitation().amount(),
        weather.getPrecipitation().probability(),
        weather.getTemperature().current(),
        weather.getTemperature().comparedToDayBefore(),
        weather.getTemperature().min(),
        weather.getTemperature().max(),
        weather.getHumidity().current(),
        weather.getHumidity().comparedToDayBefore(),
        weather.getWindSpeed().speed(),
        clothes.stream()
            .map(cloth -> new RecommendClothesDto(
                    cloth.getId(),
                    cloth.getType(),
                    cloth.getAttributes().stream()
                        .map(ClothesAttribute::getAttributeValue)
                        .findFirst()
                        .orElse(""),
                    cloth.getAttributes().stream()
                        .map(clothesAttribute -> clothesAttribute.getDefinition().getName())
                        .findFirst().orElse(null)
                )
            )
            .toList()
    );
  }

}
