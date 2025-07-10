package com.fourthread.ozang.module.domain.notification.sse.service;

import com.fourthread.ozang.module.domain.notification.sse.repository.SseEmitterRepository;
import com.fourthread.ozang.module.domain.notification.sse.SseMessage;
import com.fourthread.ozang.module.domain.notification.sse.repository.SseMessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Collection;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class SseService {

    @Value("${sse.timeout:3600000}")
    private long timeout;

    private final SseEmitterRepository emitterRepository;
    private final SseMessageRepository messageRepository;

    public SseEmitter connect(UUID receiverId, UUID lastEventId) {
        SseEmitter emitter = new SseEmitter(timeout);
        emitterRepository.save(receiverId, emitter);

        emitter.onTimeout(() -> emitterRepository.delete(receiverId, emitter));
        emitter.onCompletion(() -> emitterRepository.delete(receiverId, emitter));
        emitter.onError(e -> emitterRepository.delete(receiverId, emitter));

        if (lastEventId != null) {
            messageRepository.findAllByEventIdAfterAndReceiverId(lastEventId, receiverId)
                    .forEach(msg -> msg.toEvent().forEach(ev -> {
                        try {
                            emitter.send(ev);
                        } catch (IOException ex) {
                            log.warn("복원 실패: {}", ex.getMessage());
                        }
                    }));
        }

        return emitter;
    }

    public void send(UUID receiverId, String eventName, Object data) {
        emitterRepository.findByReceiverId(receiverId).ifPresent(emitters -> {
            SseMessage message = messageRepository.save(SseMessage.create(receiverId, eventName, data));
            Set<SseEmitter.SseEventBuilder> events = message.toEvent();
            emitters.forEach(emitter -> events.forEach(event -> {
                try {
                    emitter.send(event);
                } catch (IOException e) {
                    log.error("SSE 전송 실패: {}", e.getMessage());
                }
            }));
        });
    }

    public void send(Collection<UUID> receiverIds, String eventName, Object data) {
        SseMessage message = messageRepository.save(SseMessage.create(receiverIds, eventName, data));
        Set<SseEmitter.SseEventBuilder> events = message.toEvent();
        emitterRepository.findAllByReceiverIdsIn(receiverIds)
                .forEach(emitter -> events.forEach(event -> {
                    try {
                        emitter.send(event);
                    } catch (IOException e) {
                        log.error("SSE 다중 전송 실패: {}", e.getMessage());
                    }
                }));
    }

    public void broadcast(String eventName, Object data) {
        SseMessage message = messageRepository.save(SseMessage.createBroadcast(eventName, data));
        Set<SseEmitter.SseEventBuilder> events = message.toEvent();
        emitterRepository.findAll().forEach(emitter -> events.forEach(event -> {
            try {
                emitter.send(event);
            } catch (IOException e) {
                log.error("SSE 브로드캐스트 실패: {}", e.getMessage());
            }
        }));
    }

    @Scheduled(cron = "0 */30 * * * *")
    public void cleanUp() {
        Set<SseEmitter.SseEventBuilder> ping = Set.of(SseEmitter.event().name("ping").data(""));
        emitterRepository.findAll().forEach(emitter -> ping.forEach(ev -> {
            try {
                emitter.send(ev);
            } catch (IOException e) {
                emitter.completeWithError(e);
            }
        }));
    }
}