package com.fourthread.ozang.module.domain.follow.dto;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.UUID;

@Getter
@RequiredArgsConstructor
public class FollowSummaryProjection {

    private final UUID targetUserId;
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
