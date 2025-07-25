package com.fourthread.ozang.domain.recommend.service;

import static com.fourthread.ozang.module.common.exception.ErrorCode.OPEN_API_ERROR;
import static com.fourthread.ozang.module.common.exception.ErrorCode.RECOMMENDATION_ERROR;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fourthread.ozang.module.common.exception.ErrorDetails;
import com.fourthread.ozang.module.domain.clothes.dto.response.ClothesAttributeWithDefDto;
import com.fourthread.ozang.module.domain.clothes.dto.response.OotdDto;
import com.fourthread.ozang.module.domain.clothes.entity.Clothes;
import com.fourthread.ozang.module.domain.clothes.repository.ClothesRepository;
import com.fourthread.ozang.module.domain.recommend.dto.RecommendRequest;
import com.fourthread.ozang.module.domain.recommend.dto.RecommendationDto;
import com.fourthread.ozang.module.domain.recommend.exception.OpenApiException;
import com.fourthread.ozang.module.domain.recommend.exception.RecommendationException;
import com.fourthread.ozang.module.domain.weather.entity.Weather;
import com.fourthread.ozang.module.domain.weather.exception.WeatherNotFoundException;
import com.fourthread.ozang.module.domain.weather.repository.WeatherRepository;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientException;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class RecommendClothesService {

  private final WebClient recommendationClient;
  private final ObjectMapper objectMapper;

  private final WeatherRepository weatherRepository;
  private final ClothesRepository clothesRepository;

  @Value("${openai.prompt.id}")
  private String promptId;

  @Value("${openai.prompt.version}")
  private String promptVersion;

  /**
   * @methodName : recommend
   * @date : 2025-07-17 오후 1:24
   * @author : wongil
   * @Description: 의상 추천
   **/
  @Cacheable(
      value = "recommendation",
      cacheManager = "recommendationCacheManager",
      key = "'recommendation::'.concat(#weatherId.toString()).concat('::').concat(#userId.toString())"
  )
  public RecommendationDto recommend(UUID weatherId, UUID userId) throws JsonProcessingException {

    Weather weather = getWeather(weatherId);
    List<Clothes> userClothes = clothesRepository.findAllByOwnerIdWithAttributes(userId);

    RecommendRequest request = RecommendRequest.of(weather, userClothes);

    String inputData = objectMapper.writeValueAsString(request);
    Map<String, Object> openAiRequest = Map.of(
        "prompt", Map.of(
            "id", promptId,
            "version", promptVersion
        ),
        "input", inputData
    );
    log.info("전체 추천 데이터 = {}", inputData);

    String response = recommendationClient.post()
        .bodyValue(openAiRequest)
        .retrieve()
        .onStatus(
            status -> status.is4xxClientError() || status.is5xxServerError(),
            clientResponse -> clientResponse.bodyToMono(String.class)
                .map(errorBody -> {
                  log.error("OpenAI API 에러 응답: {}", errorBody);
                  return openApiException();
                })
        )
        .bodyToMono(String.class)
        .timeout(Duration.ofSeconds(30))
        .onErrorMap(WebClientException.class, ex -> {
          log.error("WebClient 에러 발생", ex);
          return recommendationException(ex);
        })
        .block();

    log.info("의상 추천 응답 = {}", response);

    String recommendation = extractRecommendationFromResponse(response);
    Map<String, UUID> parsedJson = objectMapper.readValue(
        recommendation,
        new TypeReference<>() {
        }
    );

    List<UUID> clothesIds = new ArrayList<>(parsedJson.values());
    List<OotdDto> ootdDtoList = getOotdDtoList(clothesIds);

    return RecommendationDto.builder()
        .weatherId(weatherId)
        .userId(userId)
        .clothes(ootdDtoList)
        .build();
  }

  @NotNull
  private RecommendationException recommendationException(WebClientException ex) {
    return new RecommendationException(
        RECOMMENDATION_ERROR.getCode(),
        RECOMMENDATION_ERROR.getMessage(),
        new ErrorDetails(
            this.getClass().getSimpleName(),
            ex.getMessage()
        )
    );
  }

  @NotNull
  private OpenApiException openApiException() {
    return new OpenApiException(
        OPEN_API_ERROR.getCode(),
        OPEN_API_ERROR.getMessage(),
        new ErrorDetails(
            this.getClass().getSimpleName(),
            OPEN_API_ERROR.name()
        )
    );
  }

  private String extractRecommendationFromResponse(String response) throws JsonProcessingException {
    try {
      Map<String, Object> responseMap = objectMapper.readValue(response, Map.class);

      String result = extractFromOutput(responseMap)
          .or(() -> extractFromContent(responseMap))
          .or(() -> extractFromResult(responseMap))
          .or(() -> extractFromChoices(responseMap))
          .or(() -> extractFromDirectResponse(responseMap, response))
          .orElse(response);

      validateExtractedResult(result);
      return result;

    } catch (Exception e) {
      log.warn("응답 파싱 실패, 원본 응답 사용: {}", response);
      return response;
    }
  }

  private Optional<String> extractFromOutput(Map<String, Object> responseMap) {
    return Optional.ofNullable(responseMap.get("output"))
        .filter(List.class::isInstance)
        .map(output -> (List<Map<String, Object>>) output)
        .filter(list -> !list.isEmpty())
        .map(list -> list.get(0))
        .flatMap(this::extractFromContentArray);
  }

  private Optional<String> extractFromContentArray(Map<String, Object> outputItem) {
    return Optional.ofNullable(outputItem.get("content"))
        .filter(List.class::isInstance)
        .map(content -> (List<Map<String, Object>>) content)
        .filter(list -> !list.isEmpty())
        .map(list -> list.get(0))
        .map(item -> (String) item.get("text"));
  }

  private Optional<String> extractFromContent(Map<String, Object> responseMap) {
    return Optional.ofNullable(responseMap.get("content"))
        .filter(String.class::isInstance)
        .map(String.class::cast);
  }

  private Optional<String> extractFromResult(Map<String, Object> responseMap) {
    Object result = responseMap.get("result");

    if (result instanceof String) {
      return Optional.of((String) result);
    }

    if (result instanceof Map) {
      Map<String, Object> resultMap = (Map<String, Object>) result;
      return Optional.ofNullable(resultMap.get("content"))
          .filter(String.class::isInstance)
          .map(String.class::cast);
    }

    return Optional.empty();
  }

  private Optional<String> extractFromChoices(Map<String, Object> responseMap) {
    return Optional.ofNullable(responseMap.get("choices"))
        .filter(List.class::isInstance)
        .map(choices -> (List<Map<String, Object>>) choices)
        .filter(list -> !list.isEmpty())
        .map(list -> list.get(0))
        .flatMap(this::extractFromChoice);
  }

  private Optional<String> extractFromChoice(Map<String, Object> choice) {
    // message.content 시도
    Optional<String> messageContent = Optional.ofNullable(choice.get("message"))
        .filter(Map.class::isInstance)
        .map(message -> (Map<String, Object>) message)
        .map(message -> (String) message.get("content"));

    if (messageContent.isPresent()) {
      return messageContent;
    }

    return Optional.ofNullable(choice.get("text"))
        .filter(String.class::isInstance)
        .map(String.class::cast);
  }

  private Optional<String> extractFromDirectResponse(Map<String, Object> responseMap,
      String originalResponse) {
    boolean hasClothingKeys = responseMap.containsKey("TOP") || responseMap.containsKey("DRESS");
    return hasClothingKeys ? Optional.of(originalResponse) : Optional.empty();
  }

  private void validateExtractedResult(String result) {
    if (result.contains("\"..\"") || result.contains("\"..\",")) {
      log.warn("잘못된 Open API 응답 = {}", result);
      throw openApiException();
    }

    log.info("추천 결과 추출: {}", result);
  }

  private List<OotdDto> getOotdDtoList(List<UUID> clothesIds) {
    return clothesRepository.findAllByIdInWithAttributes(clothesIds).stream()
        .map(clothes -> new OotdDto(
            clothes.getId(),
            clothes.getName(),
            clothes.getImageUrl(),
            clothes.getType(),
            getClothesAttributeWithDefDtos(clothes)
        ))
        .toList();
  }

  private List<ClothesAttributeWithDefDto> getClothesAttributeWithDefDtos(Clothes clothes) {
    return clothes.getAttributes().stream()
        .map(attribute -> new ClothesAttributeWithDefDto(
            attribute.getDefinition().getId(),
            attribute.getDefinition().getName(),
            attribute.getDefinition().getSelectableValues(),
            attribute.getAttributeValue()
        ))
        .toList();
  }

  private Weather getWeather(UUID weatherId) {
    return weatherRepository.findById(weatherId)
        .orElseThrow(WeatherNotFoundException::new);
  }

}