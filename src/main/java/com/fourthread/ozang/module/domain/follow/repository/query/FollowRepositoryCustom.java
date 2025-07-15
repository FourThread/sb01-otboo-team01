package com.fourthread.ozang.module.domain.follow.repository.query;

import com.fourthread.ozang.module.domain.clothes.dto.response.SortDirection;
import com.fourthread.ozang.module.domain.follow.dto.FollowSummaryProjection;
import com.fourthread.ozang.module.domain.follow.entity.Follow;

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
}
