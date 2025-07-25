package com.fourthread.ozang.domain.follow.dto;

import com.fourthread.ozang.domain.user.dto.data.UserSummary;
import java.util.UUID;

public record FollowDto(
        UUID id,
        UserSummary follower,
        UserSummary followee
) {}
