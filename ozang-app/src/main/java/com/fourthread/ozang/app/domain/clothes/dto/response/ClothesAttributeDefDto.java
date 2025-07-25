package com.fourthread.ozang.app.domain.clothes.dto.response;

import java.util.List;
import java.util.UUID;

public record ClothesAttributeDefDto(
        UUID id,
        String name,
        List<String> selectableValues
) {

}
