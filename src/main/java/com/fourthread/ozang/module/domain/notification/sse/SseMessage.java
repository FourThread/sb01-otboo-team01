package com.fourthread.ozang.module.domain.notification.sse;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class SseMessage {

    private UUID eventId;
    private Set<UUID> receiverIds;
    private boolean broadcast;
    private String eventName;
    private Object eventData;

    public static SseMessage create(UUID receiverId, String eventName, Object data) {
        return new SseMessage(UUID.randomUUID(), Set.of(receiverId), false, eventName, data);
    }

    public static SseMessage create(Collection<UUID> receiverIds, String eventName, Object data) {
        return new SseMessage(UUID.randomUUID(), new HashSet<>(receiverIds), false, eventName, data);
    }

    public static SseMessage createBroadcast(String eventName, Object data) {
        return new SseMessage(UUID.randomUUID(), Set.of(), true, eventName, data);
    }

    public boolean isReceivable(UUID receiverId) {
        return broadcast || receiverIds.contains(receiverId);
    }

    public Set<SseEmitter.SseEventBuilder> toEvent() {
        return Set.of(
                SseEmitter.event()
                        .id(eventId.toString())
                        .name(eventName)
                        .data(eventData)
        );
    }
}