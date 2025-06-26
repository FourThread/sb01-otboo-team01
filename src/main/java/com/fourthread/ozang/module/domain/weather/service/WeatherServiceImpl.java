package com.fourthread.ozang.module.domain.weather.service;

import com.fourthread.ozang.module.domain.weather.client.KakaoApiClient;
import com.fourthread.ozang.module.domain.weather.client.WeatherApiClient;
import com.fourthread.ozang.module.domain.weather.dto.WeatherAPILocation;
import com.fourthread.ozang.module.domain.weather.dto.WeatherDto;
import com.fourthread.ozang.module.domain.weather.dto.external.WeatherApiResponse;
import com.fourthread.ozang.module.domain.weather.entity.GridCoordinate;
import com.fourthread.ozang.module.domain.weather.entity.Weather;
import com.fourthread.ozang.module.domain.weather.exception.InvalidCoordinateException;
import com.fourthread.ozang.module.domain.weather.exception.WeatherApiException;
import com.fourthread.ozang.module.domain.weather.exception.WeatherDataFetchException;
import com.fourthread.ozang.module.domain.weather.mapper.WeatherMapper;
import com.fourthread.ozang.module.domain.weather.repository.WeatherRepository;
import com.fourthread.ozang.module.domain.weather.util.CoordinateConverter;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WeatherServiceImpl implements WeatherService {

    private final WeatherRepository weatherRepository;
    private final WeatherMapper weatherMapper;
    private final WeatherApiClient weatherApiClient;
    private final KakaoApiClient kakaoApiClient;
    private final CoordinateConverter coordinateConverter;

    @Override
    @Transactional
    public WeatherDto getWeatherForecast(Double longitude, Double latitude) {
        log.info("날씨 정보 조회 시작 - 위도: {}, 경도: {}", latitude, longitude);

        // 1. 좌표 유효성 검증
        validateCoordinates(longitude, latitude);

        //  2. 격자 좌표 변환
        GridCoordinate gridCoordinate = coordinateConverter.convertToGrid(latitude, longitude);
        log.debug("격자 좌표 변환 완료 - X: {}, Y: {}", gridCoordinate.getX(), gridCoordinate.getY());

        // 3. 캐시된 데이터 확인 (1시간 이내)
        LocalDateTime oneHourAgo = LocalDateTime.now().minusHours(1);
        Optional<Weather> cachedWeather = weatherRepository.findLatestByGridCoordinate(
            gridCoordinate.getX(),
            gridCoordinate.getY()
        );

        if (cachedWeather.isPresent() && cachedWeather.get().getForecastedAt().isAfter(oneHourAgo)) {
            log.info("캐시된 날씨 데이터 사용");
            return weatherMapper.toDto(cachedWeather.get());
        }

        //  4. 외부 API 호출 및 저장
        Weather weather = fetchAndSaveWeatherData(latitude, longitude, gridCoordinate);

        //  날씨 변화 감지 및 알림 처리

        log.info("날씨 정보 조회 완료");
        return weatherMapper.toDto(weather);
    }

    @Override
    public WeatherAPILocation getWeatherLocation(Double longitude, Double latitude) {
        log.info("위치 정보 조회 시작 - 위도: {}, 경도: {}", latitude, longitude);

        validateCoordinates(longitude, latitude);

        //  격자 좌표 변환
        GridCoordinate gridCoordinate = coordinateConverter.convertToGrid(latitude, longitude);

        //  카카오 API로 지역명 조회
        List<String> locationNames = kakaoApiClient.getLocationNames(latitude, longitude);

        return weatherMapper.toWeatherAPILocation(
            latitude, longitude,
            gridCoordinate.getX(), gridCoordinate.getY(),
            locationNames
        );
    }

    @Transactional
    protected Weather fetchAndSaveWeatherData(Double latitude, Double longitude,
        GridCoordinate gridCoordinate) {
        try {
            //  1. 기상청 API 호출
            log.info(" 기상청 API 호출 시작");
            WeatherApiResponse apiResponse = weatherApiClient.getWeatherForecast(gridCoordinate);

            //  2. 응답 검증
            validateApiResponse(apiResponse);

            //  3. 카카오 API로 지역명 조회
            List<String> locationNames = kakaoApiClient.getLocationNames(latitude, longitude);

            //  4. WeatherAPILocation 생성
            WeatherAPILocation location = weatherMapper.toWeatherAPILocation(
                latitude, longitude,
                gridCoordinate.getX(), gridCoordinate.getY(),
                locationNames
            );

            //  5. Weather 엔티티 생성
            List<WeatherApiResponse.Item> items = apiResponse.response().body().items().item();
            Weather weather = weatherMapper.fromApiResponse(items, location);

            //  6. API 응답 해시 생성 (중복 방지)
            String responseHash = generateResponseHash(apiResponse);
            weather.setApiResponseHash(responseHash);

            //  7. 저장
            Weather savedWeather = weatherRepository.save(weather);
            log.info("날씨 데이터 저장 완료 - ID: {}", savedWeather.getId());

            return savedWeather;

        } catch (WeatherApiException | WeatherDataFetchException e) {
            log.error("날씨 데이터 조회 실패", e);
            throw e;
        } catch (Exception e) {
            log.error("예상치 못한 오류 발생", e);
            throw new WeatherDataFetchException("날씨 데이터 조회 중 오류 발생", e);
        }
    }

    private void validateCoordinates(Double longitude, Double latitude) {
        if (longitude == null || latitude == null) {
            throw new InvalidCoordinateException("경도와 위도는 필수입니다.");
        }

        // 한국 영토 범위 검증
        if (latitude < 33.0 || latitude > 43.0 || longitude < 124.0 || longitude > 132.0) {
            throw new InvalidCoordinateException("한국 영토 범위를 벗어난 좌표입니다.");
        }
    }

    private void validateApiResponse(WeatherApiResponse response) {
        if (response == null || response.response() == null) {
            throw new WeatherDataFetchException("API 응답이 null입니다.");
        }

        WeatherApiResponse.Header header = response.response().header();
        if (header == null || !"00".equals(header.resultCode())) {
            String errorMsg = header != null ? header.resultMsg() : "Unknown error";
            String resultCode = header != null ? header.resultCode() : "UNKNOWN";

            // 에러 코드별 처리
            switch (resultCode) {
                case "01" -> throw new WeatherApiException("어플리케이션 에러", resultCode);
                case "02" -> throw new WeatherApiException("데이터베이스 에러", resultCode);
                case "03" -> throw new WeatherDataFetchException("해당 조건의 데이터가 없습니다.");
                case "04" -> throw new WeatherApiException("HTTP 에러", resultCode);
                case "05" -> throw new WeatherApiException("서비스 연결 실패", resultCode);
                case "10" -> throw new InvalidCoordinateException("잘못된 요청 파라미터입니다.");
                case "30" -> throw new WeatherApiException("등록되지 않은 서비스키", resultCode);
                default -> throw new WeatherApiException("API 호출 실패: " + errorMsg, resultCode);
            }
        }

        WeatherApiResponse.Body body = response.response().body();
        if (body == null || body.items() == null || body.items().item() == null || body.items().item().isEmpty()) {
                throw new WeatherDataFetchException("날씨 데이터가 없습니다.");
        }
    }

    private String generateResponseHash(WeatherApiResponse response) {
        try {
            String data = response.toString();
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hash = md.digest(data.getBytes());
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                hexString.append(String.format("%02x", b));
            }
            return hexString.toString();
        } catch (Exception e) {
            return String.valueOf(System.currentTimeMillis());
        }
    }
}

