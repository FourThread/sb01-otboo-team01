package com.fourthread.ozang.module.domain.clothes.repository.query;


import com.fourthread.ozang.module.domain.clothes.dto.response.SortDirection;
import com.fourthread.ozang.module.domain.clothes.entity.Clothes;
import com.fourthread.ozang.module.domain.clothes.entity.ClothesType;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static com.fourthread.ozang.module.domain.clothes.entity.QClothes.*;

@Repository
@RequiredArgsConstructor
public class ClothesRepositoryImpl implements ClothesRepositoryCustom{

    private final JPAQueryFactory queryFactory;

    @Override
    public List<Clothes> findAllByCondition(UUID ownerId,
                                            String cursor,
                                            UUID idAfter,
                                            int limit,
                                            ClothesType typeEqual,
                                            String sortBy,
                                            SortDirection direction) {

        return queryFactory
                .selectFrom(clothes)
                .where(
                        clothes.ownerId.eq(ownerId),
                        eqType(typeEqual),
                        cursorCondition(cursor, idAfter, sortBy, direction)
                )
                .orderBy(getOrderSpecifiers(sortBy, direction))
                .limit(limit)
                .fetch();
    }

    private BooleanExpression eqType(ClothesType type) {
        return type != null ? clothes.type.eq(type) : null;
    }

    private BooleanExpression cursorCondition(
                                              String cursor,
                                              UUID idAfter,
                                              String sortBy,
                                              SortDirection direction) {
        if (cursor == null) return null;

        LocalDateTime cursorTime = LocalDateTime.parse(cursor);

        if ("CREATED_AT".equalsIgnoreCase(sortBy)) {
            BooleanExpression timeCondition;
            BooleanExpression idCondition;

            if (direction.isAsc()) {
                timeCondition = clothes.createdAt.gt(cursorTime);
                idCondition = clothes.createdAt.eq(cursorTime)
                        .and(idAfter != null ? clothes.id.gt(idAfter) : null);
            } else {
                timeCondition = clothes.createdAt.lt(cursorTime);
                idCondition = clothes.createdAt.eq(cursorTime)
                        .and(idAfter != null ? clothes.id.lt(idAfter) : null);
            }

            return timeCondition.or(idCondition);
        }

        return null;
    }

    private OrderSpecifier<?>[] getOrderSpecifiers(String sortBy, SortDirection direction) {
        if ("CREATED_AT".equalsIgnoreCase(sortBy)) {
            return direction.isAsc()
                    ? new OrderSpecifier[] {
                    clothes.createdAt.asc().nullsLast(),
                    clothes.id.asc()
            }
                    : new OrderSpecifier[] {
                    clothes.createdAt.desc().nullsLast(),
                    clothes.id.desc()
            };
        }
        return new OrderSpecifier[0];
    }

    @Override
    public int countByOwnerAndType(UUID ownerId, ClothesType typeEqual) {
        return Math.toIntExact(queryFactory
                .select(clothes.count())
                .from(clothes)
                .where(
                        clothes.ownerId.eq(ownerId),
                        eqType(typeEqual)
                )
                .fetchOne());
    }
}
