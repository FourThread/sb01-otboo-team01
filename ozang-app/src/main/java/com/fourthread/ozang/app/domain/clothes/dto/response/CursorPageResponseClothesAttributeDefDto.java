package com.fourthread.ozang.app.domain.clothes.dto.response;

import com.fourthread.ozang.core.domain.clothes.dto.response.ClothesAttributeDefDto;
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
