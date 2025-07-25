package com.fourthread.ozang.app.domain.notification.event;

import com.fourthread.ozang.app.domain.clothes.dto.response.ClothesAttributeDefDto;

import java.util.UUID;

public record ClothesAttributeAddedEvent(
        ClothesAttributeDefDto clothesAttributeDefDto,
        UUID requesterId
) {
}
