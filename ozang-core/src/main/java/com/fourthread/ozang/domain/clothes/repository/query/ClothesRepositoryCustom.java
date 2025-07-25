package com.fourthread.ozang.domain.clothes.repository.query;

import com.fourthread.ozang.domain.clothes.dto.response.SortDirection;
import com.fourthread.ozang.domain.clothes.entity.Clothes;
import com.fourthread.ozang.domain.clothes.entity.ClothesType;
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
