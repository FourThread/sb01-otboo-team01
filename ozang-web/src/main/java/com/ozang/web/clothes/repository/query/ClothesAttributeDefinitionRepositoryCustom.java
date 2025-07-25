package com.ozang.web.clothes.repository.query;

import com.ozang.web.clothes.dto.response.SortBy;
import com.ozang.web.clothes.dto.response.SortDirection;
import com.ozang.web.clothes.entity.ClothesAttributeDefinition;

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
