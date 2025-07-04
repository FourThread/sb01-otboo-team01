package com.fourthread.ozang.module.domain.weather.service;

import com.fourthread.ozang.module.domain.weather.dto.WeatherDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NotificationService {
    private final NotificationRepository repo;
    private final SseEmitterService sse;  // SSE 전송
    private final PushService push;        // Web Push 전송

    @Transactional
    public void sendRainStart(WeatherDto dto) {
        var note = Notification.builder()
            .weatherId(dto.id())
            .forecastAt(dto.forecastAt())
            .x(dto.location().x())
            .y(dto.location().y())
            .type(NotificationType.RAIN_START)
            .message("비가 시작되었습니다.")
            .build();
        repo.save(note);
        sse.send(note); push.send(note);
    }

    @Transactional
    public void sendTempSpike(WeatherDto dto, double delta) {
        var note = Notification.builder()
            .weatherId(dto.id())
            .forecastAt(dto.forecastAt())
            .x(dto.location().x())
            .y(dto.location().y())
            .type(NotificationType.TEMP_SPIKE)
            .message("기온이 " + delta + "℃ 상승했습니다.")
            .build();
        repo.save(note);
        sse.send(note); push.send(note);
    }
}
