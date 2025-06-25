package com.fourthread.ozang.module.domain.weather.service;

import com.fourthread.ozang.module.domain.weather.client.KakaoApiClient;
import com.fourthread.ozang.module.domain.weather.client.WeatherApiClient;
import com.fourthread.ozang.module.domain.weather.dto.WeatherAPILocation;
import com.fourthread.ozang.module.domain.weather.dto.WeatherApiResponse;
import com.fourthread.ozang.module.domain.weather.dto.WeatherDto;
import com.fourthread.ozang.module.domain.weather.entity.GridCoordinate;
import com.fourthread.ozang.module.domain.weather.entity.Weather;
import com.fourthread.ozang.module.domain.weather.mapper.WeatherMapper;
import com.fourthread.ozang.module.domain.weather.repository.WeatherDataRepository;
import com.fourthread.ozang.module.domain.weather.util.CoordinateConverter;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class WeatherService {

    private final WeatherDataRepository weatherDataRepository;
    private final WeatherApiClient weatherApiClient;
    private final KakaoApiClient kakaoApiClient;
    private final CoordinateConverter coordinateConverter;
    private final WeatherMapper weatherMapper;

    public WeatherDto getWeatherForecast(Double latitude, Double longitude) {
        log.info("🌤️ 날씨 조회 시작 - 요청 좌표: lat={}, lon={}", latitude, longitude);

        // 1. 좌표 변환 (캐시 조회 전에 먼저 수행)
        GridCoordinate coordinate = coordinateConverter.convertToGrid(latitude, longitude);
        log.info("🗺️ 격자 좌표 변환: ({}, {}) → x={}, y={}",
            latitude, longitude, coordinate.getX(), coordinate.getY());

        // 2. 격자 좌표 기반으로 캐시 확인 (더 정확함)
        Optional<Weather> existingData = weatherDataRepository.findLatestByGridCoordinate(
            coordinate.getX(), coordinate.getY());

        if (existingData.isPresent() && isDataFresh(existingData.get())) {
            log.info("✅ 캐시된 데이터 사용 - ID: {}, 격자: x={}, y={}",
                existingData.get().getId(), coordinate.getX(), coordinate.getY());

            WeatherDto cachedResult = weatherMapper.toDto(existingData.get());

            log.info("🔍 캐시 데이터 좌표: lat={}, lon={} (격자: x={}, y={})",
                cachedResult.location().latitude(),
                cachedResult.location().longitude(),
                cachedResult.location().x(),
                cachedResult.location().y());

            return cachedResult;
        }

        log.info("🔄 새로운 데이터 조회 시작 (캐시 없음 또는 오래됨)");
        return fetchAndSaveWeatherData(latitude, longitude);
    }

    public WeatherAPILocation getWeatherLocation(Double latitude, Double longitude) {
        log.info("📍 위치 정보 조회 - lat={}, lon={}", latitude, longitude);

        GridCoordinate coordinate = coordinateConverter.convertToGrid(latitude, longitude);
        log.info("🗺️ 격자 좌표 변환: ({}, {}) → x={}, y={}",
            latitude, longitude, coordinate.getX(), coordinate.getY());

        List<String> locationNames = kakaoApiClient.getLocationNames(latitude, longitude);
        log.info("🏘️ 지역명 조회: {}", locationNames);

        WeatherAPILocation result = new WeatherAPILocation(
            latitude, longitude,
            coordinate.getX(), coordinate.getY(),
            locationNames
        );

        log.info("✅ 위치 정보 생성 완료: {}", result);
        return result;
    }

    @Transactional
    public WeatherDto fetchAndSaveWeatherData(Double latitude, Double longitude) {
        log.info("🌐 기상청 API 호출 시작 - lat={}, lon={}", latitude, longitude);

        // 1. 좌표 변환
        GridCoordinate coordinate = coordinateConverter.convertToGrid(latitude, longitude);
        log.info("🗺️ 격자 좌표: x={}, y={}", coordinate.getX(), coordinate.getY());

        // 2. 지역명 조회
        List<String> locationNames = kakaoApiClient.getLocationNames(latitude, longitude);
        log.info("🏘️ 지역명: {}", locationNames);

        // 3. WeatherAPILocation 생성
        WeatherAPILocation location = new WeatherAPILocation(
            latitude, longitude,
            coordinate.getX(), coordinate.getY(),
            locationNames
        );

        try {
            // 4. 기상청 API 호출
            log.info("☁️ 기상청 API 호출 중...");
            WeatherApiResponse response = weatherApiClient.getWeatherForecast(coordinate);

            // =============== 안전한 null 체크 추가 ===============
            if (response == null) {
                log.error("❌ 기상청 API 응답이 null입니다");
                throw new RuntimeException("Weather API response is null");
            }

            log.info("📡 API 응답 받음");

            // =============== 상세한 응답 구조 디버깅 ===============
            log.info("🔍 응답 객체: {}", response);
            log.info("🔍 response.response(): {}", response.response());
            log.info("🔍 response.header(): {}", response.header());
            log.info("🔍 response.body(): {}", response.body());

            // =============== 새로운 안전한 접근 방식 사용 ===============
            List<WeatherApiResponse.WeatherItem> items = response.getItems();
            String resultCode = response.getResultCode();

            log.info("📊 응답 결과 코드: {}", resultCode);
            log.info("📊 items 개수: {}", items != null ? items.size() : "null");

            // =============== 결과 코드가 null인 경우 더 자세히 확인 ===============
            if (resultCode == null) {
                log.warn("⚠️ 결과 코드가 null입니다. 응답 구조를 더 자세히 분석합니다.");

                // 원시 응답 구조 분석 시도
                if (response.response() != null) {
                    var responseData = response.response();
                    log.info("🔍 responseData.header(): {}", responseData.header());
                    if (responseData.header() != null) {
                        log.info("🔍 responseData.header().resultCode(): {}", responseData.header().resultCode());
                    }
                }

                if (response.header() != null) {
                    log.info("🔍 direct header.resultCode(): {}", response.header().resultCode());
                }

                // 일단 데이터가 있으면 계속 진행해보기
                if (!items.isEmpty()) {
                    log.info("💡 결과 코드는 null이지만 데이터가 있으므로 계속 진행합니다.");
                } else {
                    throw new RuntimeException("No result code and no items in API response");
                }
            } else if (!"00".equals(resultCode)) {
                log.error("❌ 기상청 API 오류 - 결과코드: {}", resultCode);
                throw new RuntimeException("Weather API error: " + resultCode);
            }

            if (items.isEmpty()) {
                log.warn("⚠️ 기상청 API 응답 데이터 없음: lat={}, lon={}", latitude, longitude);
                throw new RuntimeException("No weather data in API response");
            }

            log.info("📊 날씨 데이터 항목 수: {}", items.size());

            // 5. Weather 엔티티 생성
            Weather weather = weatherMapper.fromApiResponse(items, location);

            // 6. API 응답 해시 생성 및 중복 확인
            weather.setApiResponseHash(generateHash(response));

            log.info("🔍 생성된 Weather 엔티티 좌표: lat={}, lon={}",
                weather.getLatitude(), weather.getLongitude());

            if (!weatherDataRepository.existsByApiResponseHash(weather.getApiResponseHash())) {
                weather = weatherDataRepository.save(weather);
                log.info("💾 새 날씨 데이터 저장 완료 - ID: {}, 지역: {}",
                    weather.getId(), locationNames);
            } else {
                log.info("🔄 동일한 데이터 이미 존재 - 해시: {}", weather.getApiResponseHash());
            }

            WeatherDto result = weatherMapper.toDto(weather);

            log.info("✅ 최종 결과 좌표: lat={}, lon={}",
                result.location().latitude(),
                result.location().longitude());

            return result;

        } catch (Exception e) {
            log.error("❌ 기상청 API 호출 실패: lat={}, lon={}", latitude, longitude, e);
            throw new RuntimeException("Weather data fetch failed", e);
        }
    }

    @Transactional(readOnly = true)
    public List<Weather> getWeatherDataByTimeRange(Double latitude, Double longitude,
        LocalDateTime startTime, LocalDateTime endTime) {
        GridCoordinate coordinate = coordinateConverter.convertToGrid(latitude, longitude);
        return weatherDataRepository.findByCoordinateAndTimeRange(
            coordinate.getX(), coordinate.getY(), startTime, endTime);
    }

    private boolean isDataFresh(Weather data) {
        boolean isFresh = data.getForecastedAt().isAfter(LocalDateTime.now().minusHours(3));
        log.info("🕐 데이터 신선도 체크: {} ({}시간 전)",
            isFresh ? "신선함" : "오래됨",
            java.time.Duration.between(data.getForecastedAt(), LocalDateTime.now()).toHours());
        return isFresh;
    }

    private String generateHash(WeatherApiResponse response) {
        return String.valueOf(response.hashCode());
    }

    @Transactional
    public void cleanupOldData() {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(7);
        weatherDataRepository.deleteByForecastAtBefore(cutoffDate);
        log.info("🧹 오래된 날씨 데이터 정리 완료 - 기준: {}", cutoffDate);
    }
}
//@Service
//@RequiredArgsConstructor
//@Slf4j
//public class WeatherService {
//
//    private final WeatherDataRepository weatherDataRepository;
//    private final WeatherApiClient weatherApiClient;
//    private final KakaoApiClient kakaoApiClient;
//    private final CoordinateConverter coordinateConverter;
//    private final WeatherMapper weatherMapper;
//
//    public WeatherDto getWeatherForecast(Double latitude, Double longitude) {
//        // =============== 상세한 로깅 추가 ===============
//        log.info("🌤️ 날씨 조회 시작 - 요청 좌표: lat={}, lon={}", latitude, longitude);
//
//        // 1. 좌표 변환 (캐시 조회 전에 먼저 수행)
//        GridCoordinate coordinate = coordinateConverter.convertToGrid(latitude, longitude);
//        log.info("🗺️ 격자 좌표 변환: ({}, {}) → x={}, y={}",
//            latitude, longitude, coordinate.getX(), coordinate.getY());
//
//        // 2. 격자 좌표 기반으로 캐시 확인 (더 정확함)
//        Optional<Weather> existingData = weatherDataRepository.findLatestByGridCoordinate(
//            coordinate.getX(), coordinate.getY());
//
//        if (existingData.isPresent() && isDataFresh(existingData.get())) {
//            log.info("✅ 캐시된 데이터 사용 - ID: {}, 격자: x={}, y={}",
//                existingData.get().getId(), coordinate.getX(), coordinate.getY());
//
//            WeatherDto cachedResult = weatherMapper.toDto(existingData.get());
//
//            // =============== 캐시된 데이터 좌표 확인 ===============
//            log.info("🔍 캐시 데이터 좌표: lat={}, lon={} (격자: x={}, y={})",
//                cachedResult.location().latitude(),
//                cachedResult.location().longitude(),
//                cachedResult.location().x(),
//                cachedResult.location().y());
//
//            return cachedResult;
//        }
//
//        log.info("🔄 새로운 데이터 조회 시작 (캐시 없음 또는 오래됨)");
//        return fetchAndSaveWeatherData(latitude, longitude);
//    }
//
//    public WeatherAPILocation getWeatherLocation(Double latitude, Double longitude) {
//        log.info("📍 위치 정보 조회 - lat={}, lon={}", latitude, longitude);
//
//        GridCoordinate coordinate = coordinateConverter.convertToGrid(latitude, longitude);
//        log.info("🗺️ 격자 좌표 변환: ({}, {}) → x={}, y={}",
//            latitude, longitude, coordinate.getX(), coordinate.getY());
//
//        List<String> locationNames = kakaoApiClient.getLocationNames(latitude, longitude);
//        log.info("🏘️ 지역명 조회: {}", locationNames);
//
//        WeatherAPILocation result = new WeatherAPILocation(
//            latitude, longitude,
//            coordinate.getX(), coordinate.getY(),
//            locationNames
//        );
//
//        log.info("✅ 위치 정보 생성 완료: {}", result);
//        return result;
//    }
//
//    @Transactional
//    public WeatherDto fetchAndSaveWeatherData(Double latitude, Double longitude) {
//        log.info("🌐 기상청 API 호출 시작 - lat={}, lon={}", latitude, longitude);
//
//        // 1. 좌표 변환
//        GridCoordinate coordinate = coordinateConverter.convertToGrid(latitude, longitude);
//        log.info("🗺️ 격자 좌표: x={}, y={}", coordinate.getX(), coordinate.getY());
//
//        // 2. 지역명 조회
//        List<String> locationNames = kakaoApiClient.getLocationNames(latitude, longitude);
//        log.info("🏘️ 지역명: {}", locationNames);
//
//        // 3. WeatherAPILocation 생성 (요청받은 정확한 좌표 사용)
//        WeatherAPILocation location = new WeatherAPILocation(
//            latitude, longitude,  // ⭐ 요청받은 좌표 그대로 사용
//            coordinate.getX(), coordinate.getY(),
//            locationNames
//        );
//
//        try {
//            // 4. 기상청 API 호출
//            log.info("☁️ 기상청 API 호출 중...");
//            WeatherApiResponse response = weatherApiClient.getWeatherForecast(coordinate);
//
//            if (response.response().body().items().item().isEmpty()) {
//                log.warn("⚠️ 기상청 API 응답 데이터 없음: lat={}, lon={}", latitude, longitude);
//                return null;
//            }
//
//            // 5. Weather 엔티티 생성
//            Weather weather = weatherMapper.fromApiResponse(
//                response.response().body().items().item(),
//                location  // ⭐ 정확한 location 객체 전달
//            );
//
//            // 6. API 응답 해시 생성 및 중복 확인
//            weather.setApiResponseHash(generateHash(response));
//
//            // =============== 생성된 Weather 엔티티 좌표 확인 ===============
//            log.info("🔍 생성된 Weather 엔티티 좌표: lat={}, lon={}",
//                weather.getLatitude(), weather.getLongitude());
//
//            if (!weatherDataRepository.existsByApiResponseHash(weather.getApiResponseHash())) {
//                weather = weatherDataRepository.save(weather);
//                log.info("💾 새 날씨 데이터 저장 완료 - ID: {}, 지역: {}",
//                    weather.getId(), locationNames);
//            } else {
//                log.info("🔄 동일한 데이터 이미 존재 - 해시: {}", weather.getApiResponseHash());
//            }
//
//            WeatherDto result = weatherMapper.toDto(weather);
//
//            // =============== 최종 결과 좌표 확인 ===============
//            log.info("✅ 최종 결과 좌표: lat={}, lon={}",
//                result.location().latitude(),
//                result.location().longitude());
//
//            return result;
//
//        } catch (Exception e) {
//            log.error("❌ 기상청 API 호출 실패: lat={}, lon={}", latitude, longitude, e);
//            throw new RuntimeException("Weather data fetch failed", e);
//        }
//    }
//
//    @Transactional(readOnly = true)
//    public List<Weather> getWeatherDataByTimeRange(Double latitude, Double longitude,
//        LocalDateTime startTime, LocalDateTime endTime) {
//        GridCoordinate coordinate = coordinateConverter.convertToGrid(latitude, longitude);
//        return weatherDataRepository.findByCoordinateAndTimeRange(
//            coordinate.getX(), coordinate.getY(), startTime, endTime);
//    }
//
//    private boolean isDataFresh(Weather data) {
//        boolean isFresh = data.getForecastedAt().isAfter(LocalDateTime.now().minusHours(3));
//        log.info("🕐 데이터 신선도 체크: {} ({}시간 전)",
//            isFresh ? "신선함" : "오래됨",
//            java.time.Duration.between(data.getForecastedAt(), LocalDateTime.now()).toHours());
//        return isFresh;
//    }
//
//    private String generateHash(WeatherApiResponse response) {
//        return String.valueOf(response.hashCode());
//    }
//
//    @Transactional
//    public void cleanupOldData() {
//        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(7);
//        weatherDataRepository.deleteByForecastAtBefore(cutoffDate);
//        log.info("🧹 오래된 날씨 데이터 정리 완료 - 기준: {}", cutoffDate);
//    }
//}