package com.fourthread.ozang.module.domain.clothes.repository.query;

import com.fourthread.ozang.module.domain.clothes.dto.response.SortBy;
import com.fourthread.ozang.module.domain.clothes.dto.response.SortDirection;
import com.fourthread.ozang.module.domain.clothes.entity.ClothesAttributeDefinition;
import com.fourthread.ozang.module.domain.clothes.entity.QClothesAttributeDefinition;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

import static com.fourthread.ozang.module.domain.clothes.entity.QClothesAttributeDefinition.*;

@Repository
@RequiredArgsConstructor
public class ClothesAttributeDefinitionRepositoryImpl implements ClothesAttributeDefinitionRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    public List<ClothesAttributeDefinition> findAllByCondition(
            String cursorName,
            UUID idAfter,
            int limit,
            SortBy sortBy,
            SortDirection sortDirection,
            String keywordLike
    ) {
        QClothesAttributeDefinition q = clothesAttributeDefinition;

        BooleanBuilder condition = new BooleanBuilder();

        if (keywordLike != null && !keywordLike.isBlank()) {
            condition.and(q.name.containsIgnoreCase(keywordLike));
        }

        // 커서 조건
        if (cursorName != null) {
            BooleanBuilder cursorCondition = new BooleanBuilder();
            switch (sortBy) {
                case NAME -> {
                    if (sortDirection.isDescending()) {
                        cursorCondition.or(q.name.lt(cursorName));
                        if (idAfter != null)
                            cursorCondition.or(q.name.eq(cursorName).and(q.id.lt(idAfter)));
                    } else {
                        cursorCondition.or(q.name.gt(cursorName));
                        if (idAfter != null)
                            cursorCondition.or(q.name.eq(cursorName).and(q.id.gt(idAfter)));
                    }
                }
                case ID -> {
                    UUID cursorId = UUID.fromString(cursorName); //서비스에서 이미 검증
                    if (sortDirection.isDescending()) {
                        cursorCondition.or(q.id.lt(cursorId));
                    } else {
                        cursorCondition.or(q.id.gt(cursorId));
                    }
                }
            }
            condition.and(cursorCondition);
        }

        OrderSpecifier<?> order = getOrderSpecifier(q, sortBy, sortDirection);

        return queryFactory
                .selectFrom(q)
                .where(condition)
                .orderBy(order)
                .limit(limit)
                .fetch();
    }

    public int countByCondition(String keywordLike) {
        QClothesAttributeDefinition q = clothesAttributeDefinition;

        BooleanBuilder condition = new BooleanBuilder();
        if (keywordLike != null && !keywordLike.isBlank()) {
            condition.and(q.name.containsIgnoreCase(keywordLike));
        }

        return Math.toIntExact(
                queryFactory
                        .select(q.count())
                        .from(q)
                        .where(condition)
                        .fetchOne()
        );
    }

    private OrderSpecifier<?> getOrderSpecifier(QClothesAttributeDefinition q, SortBy sortBy, SortDirection direction) {
        return switch (sortBy) {
            case NAME -> direction.isDescending() ? q.name.desc() : q.name.asc();
            case ID -> direction.isDescending() ? q.id.desc() : q.id.asc();
        };
    }
}
