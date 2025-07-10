package com.fourthread.ozang.module.domain.notification.event;

import com.fourthread.ozang.module.domain.clothes.dto.response.ClothesAttributeDefDto;

public record ClothesAttributeUpdatedEvent(
        ClothesAttributeDefDto clothesAttributeDefDto
) {
}
