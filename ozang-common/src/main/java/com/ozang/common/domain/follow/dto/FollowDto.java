package com.ozang.common.domain.follow.dto;

import com.ozang.common.domain.user.dto.data.UserSummary;

import java.util.UUID;

public record FollowDto(
        UUID id,
        UserSummary follower,
        UserSummary followee
) {}
