package com.ozang.web.follow.dto;

import com.ozang.web.user.dto.data.UserSummary;

import java.util.UUID;

public record FollowDto(
        UUID id,
        UserSummary follower,
        UserSummary followee
) {}
