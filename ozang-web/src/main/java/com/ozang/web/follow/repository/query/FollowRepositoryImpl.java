package com.fourthread.ozang.module.domain.follow.repository.query;

import com.fourthread.ozang.module.domain.clothes.dto.response.SortDirection;
import com.fourthread.ozang.module.domain.follow.dto.FollowSummaryProjection;
import com.fourthread.ozang.module.domain.follow.entity.Follow;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static com.fourthread.ozang.module.domain.follow.entity.QFollow.*;
import static com.fourthread.ozang.module.domain.user.entity.QUser.*;

@Repository
@RequiredArgsConstructor
public class FollowRepositoryImpl  implements  FollowRepositoryCustom{


    private final JPAQueryFactory queryFactory;

    @Override
    public FollowSummaryProjection findFollowSummary(UUID targetUserId, UUID currentUserId) {

        return queryFactory
                .select(Projections.constructor(
                        FollowSummaryProjection.class,

                        // 1. followerCount (타겟의 팔로워 수)
                        JPAExpressions.select(follow.count())
                                .from(follow)
                                .where(follow.followee.id.eq(targetUserId)),

                        // 2. followingCount (타겟의 팔로잉 수)
                        JPAExpressions.select(follow.count())
                                .from(follow)
                                .where(follow.follower.id.eq(targetUserId)),

                        // 3. 내가 그를 팔로우한 Follow ID
                        JPAExpressions.select(follow.id)
                                .from(follow)
                                .where(follow.follower.id.eq(currentUserId)
                                        .and(follow.followee.id.eq(targetUserId))),

                        // 4. 그가 나를 팔로우한 Follow ID
                        JPAExpressions.select(follow.id)
                                .from(follow)
                                .where(follow.follower.id.eq(targetUserId)
                                        .and(follow.followee.id.eq(currentUserId)))
                ))
                .from(follow)
                .limit(1)
                .fetchOne();
    }

    @Override
    public List<Follow> findAllFollowingsByCondition(UUID followerId,
                                                     String cursor,
                                                     UUID idAfter,
                                                     int limit,
                                                     String nameLike,
                                                     String sortBy,
                                                     SortDirection direction) {
        return queryFactory
                .selectFrom(follow)
                .join(follow.followee, user).fetchJoin()
                .where(
                        follow.follower.id.eq(followerId),
                        nameContainsFollowee(nameLike),
                        cursorCondition(cursor, idAfter, sortBy, direction)
                )
                .orderBy(getOrderSpecifiers(sortBy, direction))
                .limit(limit)
                .fetch();
    }

    private BooleanExpression nameContainsFollowee(String nameLike) {
        return nameLike != null ? follow.followee.name.containsIgnoreCase(nameLike) : null;
    }

    @Override
    public List<Follow> findAllFollowersByCondition(UUID followeeId,
                                                    String cursor,
                                                    UUID idAfter,
                                                    int limit,
                                                    String nameLike,
                                                    String sortBy,
                                                    SortDirection direction) {
        return queryFactory
                .selectFrom(follow)
                .join(follow.follower, user).fetchJoin()
                .where(
                        follow.followee.id.eq(followeeId),
                        nameContainsFollower(nameLike),
                        cursorCondition(cursor, idAfter, sortBy, direction)
                )
                .orderBy(getOrderSpecifiers(sortBy, direction))
                .limit(limit)
                .fetch();
    }

    private BooleanExpression nameContainsFollower(String nameLike) {
        return nameLike != null ? follow.follower.name.containsIgnoreCase(nameLike) : null;
    }

    private BooleanExpression cursorCondition(
            String cursor,
            UUID idAfter,
            String sortBy,
            SortDirection direction
    ) {
        if (cursor == null) return null;

        LocalDateTime cursorTime = LocalDateTime.parse(cursor);

        if ("CREATED_AT".equalsIgnoreCase(sortBy)) {
            BooleanExpression timeCondition;
            BooleanExpression idCondition;

            if (direction.isAsc()) {
                timeCondition = follow.createdAt.gt(cursorTime);
                idCondition = follow.createdAt.eq(cursorTime)
                        .and(idAfter != null ? follow.id.gt(idAfter) : null);
            } else {
                timeCondition = follow.createdAt.lt(cursorTime);
                idCondition = follow.createdAt.eq(cursorTime)
                        .and(idAfter != null ? follow.id.lt(idAfter) : null);
            }

            return timeCondition.or(idCondition);
        }

        return null;
    }

    private OrderSpecifier<?>[] getOrderSpecifiers(String sortBy, SortDirection direction) {
        if ("CREATED_AT".equalsIgnoreCase(sortBy)) {
            return direction.isAsc()
                    ? new OrderSpecifier[]{follow.createdAt.asc().nullsLast(), follow.id.asc()}
                    : new OrderSpecifier[]{follow.createdAt.desc().nullsLast(), follow.id.desc()};
        }
        return new OrderSpecifier[0];
    }


    @Override
    public int countFollowings(UUID followerId, String nameLike) {
        return Math.toIntExact(queryFactory
                .select(follow.count())
                .from(follow)
                .where(
                        follow.follower.id.eq(followerId),
                        nameContainsFollowee(nameLike)
                )
                .fetchOne());
    }


    @Override
    public int countFollowers(UUID followeeId, String nameLike) {
        return Math.toIntExact(queryFactory
                .select(follow.count())
                .from(follow)
                .where(
                        follow.followee.id.eq(followeeId),
                        nameContainsFollower(nameLike)
                )
                .fetchOne());
    }


}
