package com.fourthread.ozang.module.domain.clothes.dto.response;

import java.util.List;
import java.util.UUID;

public record ClothesAttributeWithDefDto(
        UUID definitionId,
        String definitionName,
        List<String> selectableValues,
        String value
) {
}
