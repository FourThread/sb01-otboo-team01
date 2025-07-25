package com.ozang.common.domain.notification.event;

import com.ozang.common.domain.clothes.dto.response.ClothesAttributeDefDto;

import java.util.UUID;

public record ClothesAttributeUpdatedEvent(
        ClothesAttributeDefDto clothesAttributeDefDto,
        UUID requesterId
) {
}
