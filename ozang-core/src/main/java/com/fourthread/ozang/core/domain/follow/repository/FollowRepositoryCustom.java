package com.fourthread.ozang.core.domain.follow.repository;

import com.fourthread.ozang.core.domain.clothes.dto.response.SortDirection;
import com.fourthread.ozang.core.domain.follow.dto.FollowSummaryProjection;
import com.fourthread.ozang.core.domain.follow.entity.Follow;

import java.util.List;
import java.util.UUID;

public interface FollowRepositoryCustom {
    FollowSummaryProjection findFollowSummary(UUID targetUserId, UUID currentUserId);

    List<Follow> findAllFollowingsByCondition(UUID followerId,
                                                     String cursor,
                                                     UUID idAfter,
                                                     int limit,
                                                     String nameLike,
                                                     String sortBy,
                                                     SortDirection direction);

    int countFollowings(UUID followerId, String nameLike);


    List<Follow> findAllFollowersByCondition(UUID followeeId,
                                                    String cursor,
                                                    UUID idAfter,
                                                    int limit,
                                                    String nameLike,
                                                    String sortBy,
                                                    SortDirection direction);

    int countFollowers(UUID followeeId, String nameLike);
}
