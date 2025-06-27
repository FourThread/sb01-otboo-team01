package com.fourthread.ozang.module.domain.clothes.dto.response;

import java.util.List;
import java.util.UUID;

public record CursorPageResponseClothesAttributeDefDto(
        List<ClothesAttributeDefDto> data,
        String nextCursor,
        UUID nextIdAfter,
        boolean hasNext,
        int totalCount,
        String sortBy,
        String sortDirection
) {
}
