package com.fourthread.ozang.module.common.exception;

import com.fourthread.ozang.module.domain.weather.exception.InvalidCoordinateException;
import com.fourthread.ozang.module.domain.weather.exception.WeatherApiException;
import com.fourthread.ozang.module.domain.weather.exception.WeatherDataFetchException;
import com.fourthread.ozang.module.domain.weather.exception.WeatherNotFoundException;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(WeatherNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleWeatherNotFoundException(WeatherNotFoundException e) {
        log.warn("Weather not found: {}", e.getMessage());

        ErrorResponse errorResponse = new ErrorResponse(
            "WeatherNotFoundException",
            e.getMessage(),
            Map.of("timestamp", System.currentTimeMillis())
        );

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }

    @ExceptionHandler(InvalidCoordinateException.class)
    public ResponseEntity<ErrorResponse> handleInvalidCoordinateException(InvalidCoordinateException e) {
        log.warn("Invalid coordinate: {}", e.getMessage());

        ErrorResponse errorResponse = new ErrorResponse(
            "InvalidCoordinateException",
            e.getMessage(),
            Map.of("timestamp", System.currentTimeMillis())
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    @ExceptionHandler(WeatherDataFetchException.class)
    public ResponseEntity<ErrorResponse> handleWeatherDataFetchException(WeatherDataFetchException e) {
        log.error("Weather data fetch failed: {}", e.getMessage(), e);

        Map<String, String> details = new HashMap<>();
        details.put("timestamp", String.valueOf(System.currentTimeMillis()));
        details.put("cause", e.getCause() != null ? e.getCause().getClass().getSimpleName() : "Unknown");

        ErrorResponse errorResponse = new ErrorResponse(
            "WeatherDataFetchException",
            "날씨 데이터를 가져오는데 실패했습니다.",
            details
        );

        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(errorResponse);
    }

    @ExceptionHandler(WeatherApiException.class)
    public ResponseEntity<ErrorResponse> handleWeatherApiException(WeatherApiException e) {
        log.error("Weather API error: {} (Code: {})", e.getMessage(), e.getResultCode());

        Map<String, String> details = new HashMap<>();
        details.put("timestamp", String.valueOf(System.currentTimeMillis()));
        details.put("resultCode", e.getResultCode());

        ErrorResponse errorResponse = new ErrorResponse(
            "WeatherApiException",
            "외부 날씨 API 호출 중 오류가 발생했습니다.",
            details
        );

        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(errorResponse);
    }

    // =============== 일반 예외 처리 ===============

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(IllegalArgumentException e) {
        log.warn("Illegal argument: {}", e.getMessage());

        ErrorResponse errorResponse = new ErrorResponse(
            "IllegalArgumentException",
            e.getMessage(),
            Map.of("timestamp", System.currentTimeMillis())
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResponse> handleRuntimeException(RuntimeException e) {
        log.error("Unexpected runtime exception: {}", e.getMessage(), e);

        ErrorResponse errorResponse = new ErrorResponse(
            "RuntimeException",
            "예상치 못한 오류가 발생했습니다.",
            Map.of("timestamp", System.currentTimeMillis())
        );

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception e) {
        log.error("Unexpected exception: {}", e.getMessage(), e);

        ErrorResponse errorResponse = new ErrorResponse(
            "Exception",
            "서버 내부 오류가 발생했습니다.",
            Map.of("timestamp", System.currentTimeMillis())
        );

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }

    public record ErrorResponse(
        String exceptionName,
        String message,
        Map<String, ?> details
    ) {}
}
