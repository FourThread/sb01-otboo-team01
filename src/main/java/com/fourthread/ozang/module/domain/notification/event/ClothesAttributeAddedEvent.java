package com.fourthread.ozang.module.domain.notification.event;

import com.fourthread.ozang.module.domain.clothes.dto.response.ClothesAttributeDefDto;

import java.util.UUID;

public record ClothesAttributeAddedEvent(
        ClothesAttributeDefDto clothesAttributeDefDto,
        UUID ownerId
) {
}
