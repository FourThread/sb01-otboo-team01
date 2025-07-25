package com.fourthread.ozang.module.domain.notification.sse.repository;

import org.springframework.stereotype.Repository;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Repository
public class SseEmitterRepository {

    private final Map<UUID, List<SseEmitter>> data = new ConcurrentHashMap<>();

    public SseEmitter save(UUID receiverId, SseEmitter emitter) {
        data.computeIfAbsent(receiverId, id -> new CopyOnWriteArrayList<>()).add(emitter);
        return emitter;
    }

    public Optional<List<SseEmitter>> findByReceiverId(UUID receiverId) {
        return Optional.ofNullable(data.get(receiverId));
    }

    public List<SseEmitter> findAllByReceiverIdsIn(Collection<UUID> receiverIds) {
        return data.entrySet().stream()
                .filter(e -> receiverIds.contains(e.getKey()))
                .flatMap(e -> e.getValue().stream())
                .toList();
    }

    public List<SseEmitter> findAll() {
        return data.values().stream().flatMap(Collection::stream).toList();
    }

    public void delete(UUID receiverId, SseEmitter emitter) {
        List<SseEmitter> emitters = data.get(receiverId);
        if (emitters != null) {
            emitters.remove(emitter);
        }
    }
}