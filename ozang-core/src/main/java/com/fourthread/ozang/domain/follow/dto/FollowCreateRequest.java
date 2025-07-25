package com.fourthread.ozang.domain.follow.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record FollowCreateRequest(

        @NotNull(message = "팔로우 대상 ID는 필수입니다.")
        UUID followeeId,

        @NotNull(message = "팔로워 ID는 필수입니다.")
        UUID followerId
) {}