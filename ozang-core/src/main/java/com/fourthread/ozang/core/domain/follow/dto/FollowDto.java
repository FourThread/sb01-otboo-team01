package com.fourthread.ozang.core.domain.follow.dto;

import com.fourthread.ozang.core.domain.user.dto.data.UserSummary;

import java.util.UUID;

public record FollowDto(
        UUID id,
        UserSummary follower,
        UserSummary followee
) {}
