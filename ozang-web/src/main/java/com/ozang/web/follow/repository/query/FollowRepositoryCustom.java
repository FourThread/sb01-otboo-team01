package com.ozang.web.follow.repository.query;

import com.ozang.web.clothes.dto.response.SortDirection;
import com.ozang.web.follow.dto.FollowSummaryProjection;
import com.ozang.web.follow.entity.Follow;

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
