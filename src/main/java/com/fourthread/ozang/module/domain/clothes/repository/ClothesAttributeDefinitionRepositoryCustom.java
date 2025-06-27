package com.fourthread.ozang.module.domain.clothes.repository;

import com.fourthread.ozang.module.domain.clothes.dto.response.SortBy;
import com.fourthread.ozang.module.domain.clothes.dto.response.SortDirection;
import com.fourthread.ozang.module.domain.clothes.entity.ClothesAttributeDefinition;

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
