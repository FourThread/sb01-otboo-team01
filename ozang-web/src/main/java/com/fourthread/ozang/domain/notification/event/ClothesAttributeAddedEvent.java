package com.fourthread.ozang.domain.notification.event;

import com.fourthread.ozang.domain.clothes.dto.response.ClothesAttributeDefDto;
import java.util.UUID;

public record ClothesAttributeAddedEvent(
        ClothesAttributeDefDto clothesAttributeDefDto,
        UUID requesterId
) {
}
