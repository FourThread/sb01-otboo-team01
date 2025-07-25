package com.ozang.web.clothes.repository.query;

import com.ozang.web.clothes.dto.response.SortDirection;
import com.ozang.web.clothes.entity.Clothes;
import com.ozang.web.clothes.entity.ClothesType;

import java.util.List;
import java.util.UUID;

public interface ClothesRepositoryCustom {

    List<Clothes> findAllByCondition(
            UUID ownerId,
            String cursor,
            UUID idAfter,
            int limit,
            ClothesType typeEqual,
            String sortBy,
            SortDirection direction
    );

    int countByOwnerAndType(UUID ownerId, ClothesType typeEqual);
}
