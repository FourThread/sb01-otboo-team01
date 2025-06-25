package com.fourthread.ozang.module.domain.weather.controller;

import com.fourthread.ozang.module.domain.weather.dto.WeatherAPILocation;
import com.fourthread.ozang.module.domain.weather.dto.WeatherDto;
import com.fourthread.ozang.module.domain.weather.service.WeatherService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/weathers")
@Slf4j
@RequiredArgsConstructor
@Tag(name = "날씨 관리", description = "날씨 관련 API")
public class WeatherController {

    private final WeatherService weatherService;

    /**
     * 날씨 정보 조회
     */
    @GetMapping
    @Operation(summary = "날씨 정보 조회", description = "날씨 정보 조회 API")
    public ResponseEntity<WeatherDto> getWeather(
        @RequestParam @NotNull @Schema(description = "경도") Double longitude,
        @RequestParam @NotNull @Schema(description = "위도") Double latitude) {

        log.debug("Weather data requested for coordinates: lat={}, lon={}", latitude, longitude);

        WeatherDto weatherData = weatherService.getWeatherForecast(latitude, longitude);

        return ResponseEntity.ok(weatherData);
    }

    /**
     * 날씨 위치 정보 조회
     */
    @GetMapping("/location")
    @Operation(summary = "날씨 위치 정보 조회", description = "날씨 위치 정보 조회 API")
    public ResponseEntity<WeatherAPILocation> getWeatherLocation(
        @RequestParam @NotNull @Schema(description = "경도") Double longitude,
        @RequestParam @NotNull @Schema(description = "위도") Double latitude) {

        log.debug("Weather location requested for coordinates: lat={}, lon={}", latitude, longitude);

        WeatherAPILocation location = weatherService.getWeatherLocation(latitude, longitude);

        return ResponseEntity.ok(location);
    }

    @GetMapping("/weather-page")
    @ResponseBody
    public ResponseEntity<String> weatherTestPage() {
        String html = """
        <!DOCTYPE html>
        <html lang="ko">
        <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <title>🌤️ 날씨 테스트</title>
            <style>
                body { font-family: Arial, sans-serif; max-width: 600px; margin: 50px auto; padding: 20px; }
                .btn { background: #007bff; color: white; border: none; padding: 15px 30px; border-radius: 5px; cursor: pointer; margin: 10px; }
                .btn:hover { background: #0056b3; }
                .result { margin: 20px 0; padding: 15px; border: 1px solid #ddd; border-radius: 5px; }
                .loading { color: #007bff; }
                .success { color: #28a745; background: #f8f9fa; }
                .error { color: #dc3545; background: #f8d7da; }
            </style>
        </head>
        <body>
            <h1>🌤️ 날씨 API 테스트</h1>
            
            <div>
                <button class="btn" onclick="getCurrentWeather()">📍 현재 위치 날씨</button>
                <button class="btn" onclick="getSeoulWeather()">🏙️ 서울 날씨</button>
                <button class="btn" onclick="getBusanWeather()">🌊 부산 날씨</button>
            </div>
            
            <div id="result" class="result"></div>
            
            <script>
                const resultDiv = document.getElementById('result');
                
                function showResult(content, type = 'success') {
                    resultDiv.innerHTML = content;
                    resultDiv.className = `result ${type}`;
                }
                
                async function fetchWeather(lat, lon, locationName = '현재 위치') {
                    try {
                        showResult(`🔍 ${locationName} 날씨를 조회하는 중...`, 'loading');
                        
                        const response = await fetch(`/api/weathers?latitude=${lat}&longitude=${lon}`);
                        
                        if (!response.ok) {
                            throw new Error(`HTTP ${response.status}`);
                        }
                        
                        const data = await response.json();
                        
                        showResult(`
                            <h3>🌍 ${locationName} 날씨</h3>
                            <p><strong>📍 위치:</strong> ${data.location.locationNames.join(' > ')}</p>
                            <p><strong>🌡️ 온도:</strong> ${data.temperature.current}°C (최저 ${data.temperature.min}°C, 최고 ${data.temperature.max}°C)</p>
                            <p><strong>☁️ 날씨:</strong> ${data.skyStatus}</p>
                            <p><strong>💧 습도:</strong> ${data.humidity.current}%</p>
                            <p><strong>🌧️ 강수확률:</strong> ${data.precipitation.probability}%</p>
                            <p><strong>💨 풍속:</strong> ${data.windSpeed.speed} m/s (${data.windSpeed.asWord})</p>
                            <p><small>🕐 예보시간: ${data.forecastAt}</small></p>
                        `, 'success');
                        
                    } catch (error) {
                        showResult(`❌ 오류 발생: ${error.message}`, 'error');
                    }
                }
                
                function getCurrentWeather() {
                    if (!navigator.geolocation) {
                        showResult('❌ 브라우저에서 위치 서비스를 지원하지 않습니다.', 'error');
                        return;
                    }
                    
                    showResult('📍 위치 권한을 요청하는 중...', 'loading');
                    
                    navigator.geolocation.getCurrentPosition(
                        (position) => {
                            fetchWeather(position.coords.latitude, position.coords.longitude);
                        },
                        (error) => {
                            showResult(`❌ 위치 조회 실패: ${error.message}`, 'error');
                        }
                    );
                }
                
                function getSeoulWeather() {
                    fetchWeather(37.5665, 126.9780, '서울');
                }
                
                function getBusanWeather() {
                    fetchWeather(35.1796, 129.0756, '부산');
                }
            </script>
        </body>
        </html>
        """;

        return ResponseEntity.ok()
            .header("Content-Type", "text/html; charset=UTF-8")
            .body(html);
    }
}