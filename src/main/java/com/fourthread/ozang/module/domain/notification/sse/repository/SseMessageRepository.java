package com.fourthread.ozang.module.domain.notification.sse.repository;

import com.fourthread.ozang.module.domain.notification.sse.SseMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

@Repository
public class SseMessageRepository {

    @Value("${sse.event-queue-capacity:100}")
    private int capacity;

    private final Map<UUID, SseMessage> messages = new ConcurrentHashMap<>();
    private final Deque<UUID> eventQueue = new ConcurrentLinkedDeque<>();

    public SseMessage save(SseMessage message) {
        makeRoom();
        eventQueue.addLast(message.getEventId());
        messages.put(message.getEventId(), message);
        return message;
    }

    public List<SseMessage> findAllByEventIdAfterAndReceiverId(UUID eventId, UUID receiverId) {
        return eventQueue.stream()
                .dropWhile(id -> !id.equals(eventId))
                .skip(1)
                .map(messages::get)
                .filter(msg -> msg.isReceivable(receiverId))
                .toList();
    }

    private void makeRoom() {
        while (eventQueue.size() >= capacity) {
            UUID oldest = eventQueue.pollFirst();
            if (oldest != null) messages.remove(oldest);
        }
    }
}