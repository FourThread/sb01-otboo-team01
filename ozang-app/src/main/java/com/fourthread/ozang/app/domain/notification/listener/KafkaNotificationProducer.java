package com.fourthread.ozang.app.domain.notification.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
@Component
public class KafkaNotificationProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public void send(String topic, Object event) {
        try {
            String payload = objectMapper.writeValueAsString(event);
            kafkaTemplate.send(topic, payload);
            log.debug("Kafka 전송 완료 (key 없음): topic={}", topic);
        } catch (Exception e) {
            log.error("Kafka 전송 실패 (key 없음): topic={}, error={}", topic, e.getMessage(), e);
            throw new RuntimeException("Kafka 전송 실패", e);
        }
    }

    public void send(String topic, UUID receiverId, Object event) {
        try {
            String payload = objectMapper.writeValueAsString(event);
            String key = receiverId.toString();
            kafkaTemplate.send(topic, key, payload);
            log.debug("Kafka 전송 완료: topic={}, key={}", topic, key);
        } catch (Exception e) {
            log.error("Kafka 전송 실패: topic={}, error={}", topic, e.getMessage(), e);
            throw new RuntimeException("Kafka 전송 실패", e);
        }
    }
}