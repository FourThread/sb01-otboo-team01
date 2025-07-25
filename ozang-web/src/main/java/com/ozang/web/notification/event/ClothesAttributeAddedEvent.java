package com.ozang.web.notification.event;

import com.ozang.web.clothes.dto.response.ClothesAttributeDefDto;

import java.util.UUID;

public record ClothesAttributeAddedEvent(
        ClothesAttributeDefDto clothesAttributeDefDto,
        UUID requesterId
) {
}
