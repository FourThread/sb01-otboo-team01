package com.fourthread.ozang.domain.follow.dto;

import java.util.UUID;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class FollowSummaryProjection {

    private final long followerCount;
    private final long followingCount;
    private final UUID followedByMeId;
    private final UUID followingMeId;

    public boolean isFollowedByMe() {
        return followedByMeId != null;
    }

    public boolean isFollowingMe() {
        return followingMeId != null;
    }
}
