package com.fourthread.ozang.app.domain.clothes.repository.query;

import com.fourthread.ozang.app.domain.clothes.dto.response.SortBy;
import com.fourthread.ozang.core.domain.clothes.dto.response.SortDirection;
import com.fourthread.ozang.core.domain.clothes.entity.ClothesAttributeDefinition;

import java.util.List;
import java.util.UUID;

public interface ClothesAttributeDefinitionRepositoryCustom {

    public List<ClothesAttributeDefinition> findAllByCondition(
            String cursorName,
            UUID idAfter,
            int limit,
            SortBy sortBy,
            SortDirection sortDirection,
            String keywordLike
    );

    public int countByCondition(String keywordLike);

}
