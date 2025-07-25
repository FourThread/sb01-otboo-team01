package com.ozang.common.domain.follow.dto;

import java.util.UUID;

public record FollowSummaryDto(
        UUID followeeId, // -> 팔로우 대상 사용자 ID
        long followerCount, //-> 팔로우 대상 사용자 팔로워 count
        long followingCount, // -> 팔로우 대상 사용자 팔로인 count
        boolean followedByMe, // 내가 팔로우 대상자를 팔로우 하는지
        UUID followedByMeId, // 내가 팔로우 대상자를 팔로우 하는 경우 팔로우 ID
        boolean followingMe // 대상 사용자가 나를 팔로우 하는지
) {}