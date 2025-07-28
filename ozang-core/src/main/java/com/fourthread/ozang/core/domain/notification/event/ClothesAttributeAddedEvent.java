package com.fourthread.ozang.core.domain.notification.event;


import com.fourthread.ozang.core.domain.clothes.dto.response.ClothesAttributeDefDto;
import java.util.UUID;

public record ClothesAttributeAddedEvent(
        ClothesAttributeDefDto clothesAttributeDefDto,
        UUID requesterId
) {
}
